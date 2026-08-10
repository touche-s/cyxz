package com.cyxz.governance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

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
    public ReportVO getReportDetail(Long id) {
        ReportPO po = reportMapper.selectById(id);
        if (po == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        return toVO(po);
    }

    @Override
    public void approveReport(Long id, Long handlerId, String note) {
        ReportPO po = checkPending(id);
        po.setStatus(GovernanceConstants.STATUS_APPROVED);
        po.setHandlerId(handlerId);
        po.setHandlerNote(note);
        po.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(po);
        publishTakedownEvent(po, handlerId);
        log.info("举报审核通过: reportId={}, handlerId={}, targetType={}, targetId={}",
                id, handlerId, po.getTargetType(), po.getTargetId());
    }

    @Override
    public void rejectReport(Long id, Long handlerId, String note) {
        ReportPO po = checkPending(id);
        po.setStatus(GovernanceConstants.STATUS_REJECTED);
        po.setHandlerId(handlerId);
        po.setHandlerNote(note);
        po.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(po);
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
                    .build();
            rabbitTemplate.convertAndSend(GovernanceConstants.EXCHANGE, GovernanceConstants.ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("发布内容处置事件失败: reportId={}, targetType={}, targetId={}",
                    po.getId(), po.getTargetType(), po.getTargetId(), e);
        }
    }

    private ReportVO toVO(ReportPO po) {
        ReportVO vo = new ReportVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }
}
