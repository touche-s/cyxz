package com.cyxz.governance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.audit.api.constant.AuditConstants;
import com.cyxz.audit.api.event.AuditEvent;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.constant.AnalyticsConstants;
import com.cyxz.common.event.AnalyticsEvent;
import com.cyxz.governance.api.constant.GovernanceConstants;
import com.cyxz.governance.api.event.ContentTakedownEvent;
import com.cyxz.governance.dto.CreateReportRequest;
import com.cyxz.governance.entity.ReportPO;
import com.cyxz.governance.mapper.ReportMapper;
import com.cyxz.governance.service.ReportService;
import com.cyxz.governance.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 举报服务实现
 * <p>审核通过时发布 {@link ContentTakedownEvent}，由 post / comment 服务消费完成内容删除，
 * 实现"治理中心不直接写业务库"的最终一致性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public Long createReport(Long reporterId, CreateReportRequest request) {
        String targetType = request.getTargetType();
        if (!GovernanceConstants.TARGET_POST.equals(targetType)
                && !GovernanceConstants.TARGET_COMMENT.equals(targetType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "举报对象类型不合法");
        }
        Long exists = reportMapper.selectCount(new LambdaQueryWrapper<ReportPO>()
                .eq(ReportPO::getReporterId, reporterId)
                .eq(ReportPO::getTargetType, targetType)
                .eq(ReportPO::getTargetId, request.getTargetId()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.REPORT_DUPLICATE);
        }
        ReportPO po = new ReportPO();
        po.setReporterId(reporterId);
        po.setTargetType(targetType);
        po.setTargetId(request.getTargetId());
        po.setReason(request.getReason());
        po.setStatus(GovernanceConstants.STATUS_PENDING);
        reportMapper.insert(po);
        log.info("用户提交举报: reportId={}, reporterId={}, targetType={}, targetId={}",
                po.getId(), reporterId, targetType, request.getTargetId());
        return po.getId();
    }

    @Override
    public PageResult<ReportVO> listForAdmin(String status, String targetType, int page, int size) {
        LambdaQueryWrapper<ReportPO> wrapper = new LambdaQueryWrapper<ReportPO>()
                .eq(StringUtils.hasText(status), ReportPO::getStatus, status)
                .eq(StringUtils.hasText(targetType), ReportPO::getTargetType, targetType)
                .orderByDesc(ReportPO::getCreateTime);
        Page<ReportPO> p = reportMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords().stream().map(this::toVO).toList(), p.getTotal(), page, size);
    }

    @Override
    public PageResult<ReportVO> listMine(Long reporterId, String status, int page, int size) {
        LambdaQueryWrapper<ReportPO> wrapper = new LambdaQueryWrapper<ReportPO>()
                .eq(ReportPO::getReporterId, reporterId)
                .eq(StringUtils.hasText(status), ReportPO::getStatus, status)
                .orderByDesc(ReportPO::getCreateTime);
        Page<ReportPO> p = reportMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords().stream().map(this::toVO).toList(), p.getTotal(), page, size);
    }

    @Override
    public ReportVO getReportDetail(Long id) {
        ReportPO po = reportMapper.selectById(id);
        if (po == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        return toVO(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReport(Long id, Long handlerId, String note) {
        ReportPO po = checkPending(id);
        // 乐观锁更新：仅 PENDING → APPROVED 原子写，防并发重复审核互相覆盖
        LambdaUpdateWrapper<ReportPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReportPO::getId, id)
               .eq(ReportPO::getStatus, GovernanceConstants.STATUS_PENDING);
        ReportPO update = new ReportPO();
        update.setStatus(GovernanceConstants.STATUS_APPROVED);
        update.setHandlerId(handlerId);
        update.setHandlerNote(note);
        update.setHandledAt(LocalDateTime.now());
        int rows = reportMapper.update(update, wrapper);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_HANDLED);
        }

        // MQ 发送放到事务提交后：先保证 DB 状态落库，再发事件
        final String targetType = po.getTargetType();
        final Long targetId = po.getTargetId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishTakedownEvent(po, handlerId);
                publishAuditEvent(handlerId, AuditConstants.ACTION_REPORT_APPROVE, targetType, targetId);
                publishAnalyticsEvent(AnalyticsConstants.METRIC_REPORT_HANDLED);
            }
        });
        log.info("举报审核通过: reportId={}, handlerId={}, targetType={}, targetId={}",
                id, handlerId, targetType, targetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReport(Long id, Long handlerId, String note) {
        ReportPO po = checkPending(id);
        // 乐观锁更新：仅 PENDING → REJECTED 原子写，防并发重复审核互相覆盖
        LambdaUpdateWrapper<ReportPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReportPO::getId, id)
               .eq(ReportPO::getStatus, GovernanceConstants.STATUS_PENDING);
        ReportPO update = new ReportPO();
        update.setStatus(GovernanceConstants.STATUS_REJECTED);
        update.setHandlerId(handlerId);
        update.setHandlerNote(note);
        update.setHandledAt(LocalDateTime.now());
        int rows = reportMapper.update(update, wrapper);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_HANDLED);
        }

        // MQ 发送放到事务提交后
        final String targetType = po.getTargetType();
        final Long targetId = po.getTargetId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishAuditEvent(handlerId, AuditConstants.ACTION_REPORT_REJECT, targetType, targetId);
            }
        });
        log.info("举报审核驳回: reportId={}, handlerId={}", id, handlerId);
    }

    private ReportPO checkPending(Long id) {
        ReportPO po = reportMapper.selectById(id);
        if (po == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        if (!GovernanceConstants.STATUS_PENDING.equals(po.getStatus())) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_HANDLED);
        }
        return po;
    }

    private void publishTakedownEvent(ReportPO po, Long operatorId) {
        try {
            ContentTakedownEvent event = ContentTakedownEvent.builder()
                    .targetType(po.getTargetType())
                    .targetId(po.getTargetId())
                    .reportId(po.getId())
                    .operatorId(operatorId)
                    .reporterId(po.getReporterId())
                    .build();
            rabbitTemplate.convertAndSend(GovernanceConstants.EXCHANGE, GovernanceConstants.ROUTING_KEY, event);
        } catch (Exception e) {
            // 发送失败写入 Redis 补偿队列，由 TakedownRetryTask 定时重发，避免举报通过但内容未下架
            log.error("发布内容处置事件失败，已入补偿队列等待重试: reportId={}, targetType={}, targetId={}",
                    po.getId(), po.getTargetType(), po.getTargetId(), e);
            enqueueTakedownRetry(po, operatorId);
        }
    }

    /**
     * 将处置事件 JSON 写入 Redis 补偿队列（LPUSH），供定时任务重发。
     */
    private void enqueueTakedownRetry(ReportPO po, Long operatorId) {
        try {
            ContentTakedownEvent event = ContentTakedownEvent.builder()
                    .targetType(po.getTargetType())
                    .targetId(po.getTargetId())
                    .reportId(po.getId())
                    .operatorId(operatorId)
                    .reporterId(po.getReporterId())
                    .build();
            stringRedisTemplate.opsForList().leftPush(
                    GovernanceConstants.TAKEDOWN_FAILED_QUEUE_KEY, OBJECT_MAPPER.writeValueAsString(event));
        } catch (Exception ex) {
            log.error("写入内容处置事件补偿队列失败: reportId={}", po.getId(), ex);
        }
    }

    private void publishAuditEvent(Long operatorId, String action, String targetType, Long targetId) {
        try {
            AuditEvent auditEvent = AuditEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .operatorId(operatorId)
                    .operatorName(null)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(null)
                    .ip(null)
                    .createTime(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(AuditConstants.EXCHANGE, AuditConstants.ROUTING_KEY, auditEvent);
        } catch (Exception e) {
            log.error("发布审计事件失败: action={}, targetId={}", action, targetId, e);
        }
    }

    private void publishAnalyticsEvent(String metric) {
        try {
            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .metric(metric)
                    .value(1)
                    .statDate(LocalDate.now())
                    .build();
            rabbitTemplate.convertAndSend(AnalyticsConstants.EXCHANGE, AnalyticsConstants.ROUTING_KEY, analyticsEvent);
        } catch (Exception e) {
            log.error("发布统计事件失败: metric={}", metric, e);
        }
    }

    private ReportVO toVO(ReportPO po) {
        ReportVO vo = new ReportVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }
}
