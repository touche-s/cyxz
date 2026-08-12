package com.cyxz.circle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.audit.api.constant.AuditConstants;
import com.cyxz.audit.api.event.AuditEvent;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.cyxz.circle.constant.CircleApplicationConstants;
import com.cyxz.circle.dto.CreateCircleJoinRequest;
import com.cyxz.circle.entity.CircleJoinApplicationPO;
import com.cyxz.circle.mapper.CircleJoinApplicationMapper;
import com.cyxz.circle.service.CircleJoinApplicationService;
import com.cyxz.circle.service.CircleService;
import com.cyxz.circle.vo.CircleJoinApplicationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 入圈申请服务实现
 * <p>负责入圈申请的提交、查询与审核。审核通过时在同模块内直接调用
 * {@link CircleService#joinCircle(Long, Long)} 完成成员加入，
 * 借助同一 MySQL 实例的本地事务保证一致性，不再依赖跨模块 MQ 事件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircleJoinApplicationServiceImpl implements CircleJoinApplicationService {

    private final CircleJoinApplicationMapper applicationMapper;
    private final CircleService circleService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 提交入圈申请
     * <p>同一用户对同一圈子存在待审核申请时拒绝重复提交，状态机限定为 PENDING。
     *
     * @param applicantId 申请人用户 ID
     * @param request     入圈申请请求（circleId / reason）
     * @return 申请记录 ID
     */
    @Override
    public Long createApplication(Long applicantId, CreateCircleJoinRequest request) {
        Long exists = applicationMapper.selectCount(new LambdaQueryWrapper<CircleJoinApplicationPO>()
                .eq(CircleJoinApplicationPO::getApplicantId, applicantId)
                .eq(CircleJoinApplicationPO::getCircleId, request.getCircleId())
                .eq(CircleJoinApplicationPO::getStatus, CircleApplicationConstants.STATUS_PENDING));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CIRCLE_JOIN_APPLICATION_DUPLICATE);
        }
        CircleJoinApplicationPO po = new CircleJoinApplicationPO();
        po.setApplicantId(applicantId);
        po.setCircleId(request.getCircleId());
        po.setReason(request.getReason());
        po.setStatus(CircleApplicationConstants.STATUS_PENDING);
        applicationMapper.insert(po);
        log.info("用户提交入圈申请: applicationId={}, applicantId={}, circleId={}",
                po.getId(), applicantId, request.getCircleId());
        return po.getId();
    }

    /**
     * 查询申请人的入圈申请列表，按提交时间倒序分页
     *
     * @param applicantId 申请人用户 ID
     * @param page        页码
     * @param size        每页条数
     * @return 申请记录分页
     */
    @Override
    public PageResult<CircleJoinApplicationVO> listByApplicant(Long applicantId, int page, int size) {
        LambdaQueryWrapper<CircleJoinApplicationPO> wrapper = new LambdaQueryWrapper<CircleJoinApplicationPO>()
                .eq(CircleJoinApplicationPO::getApplicantId, applicantId)
                .orderByDesc(CircleJoinApplicationPO::getCreateTime);
        Page<CircleJoinApplicationPO> p = applicationMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords().stream().map(this::toVO).toList(), p.getTotal(), page, size);
    }

    /**
     * 管理端入圈申请列表，支持按状态/圈子筛选并分页
     *
     * @param status   状态筛选（null=全部）
     * @param circleId 圈子筛选（null=全部）
     * @param page     页码
     * @param size     每页条数
     * @return 申请记录分页
     */
    @Override
    public PageResult<CircleJoinApplicationVO> listForAdmin(String status, Long circleId, int page, int size) {
        LambdaQueryWrapper<CircleJoinApplicationPO> wrapper = new LambdaQueryWrapper<CircleJoinApplicationPO>()
                .eq(StringUtils.hasText(status), CircleJoinApplicationPO::getStatus, status)
                .eq(circleId != null, CircleJoinApplicationPO::getCircleId, circleId)
                .orderByDesc(CircleJoinApplicationPO::getCreateTime);
        Page<CircleJoinApplicationPO> p = applicationMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords().stream().map(this::toVO).toList(), p.getTotal(), page, size);
    }

    /**
     * 查询入圈申请详情
     *
     * @param id 申请记录 ID
     * @return 申请详情 VO
     */
    @Override
    public CircleJoinApplicationVO getDetail(Long id) {
        CircleJoinApplicationPO po = applicationMapper.selectById(id);
        if (po == null) {
            throw new BusinessException(ErrorCode.CIRCLE_JOIN_APPLICATION_NOT_FOUND);
        }
        return toVO(po);
    }

    /**
     * 审核通过入圈申请
     * <p>更新申请状态为 APPROVED 后，同事务内直接调用
     * {@link CircleService#joinCircle(Long, Long)} 加入成员，
     * 从而保证「审核通过」与「成员加入」的本地一致性。
     *
     * @param id          申请记录 ID
     * @param reviewerId  审核人用户 ID
     * @param reviewNote  审核意见
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveApplication(Long id, Long reviewerId, String reviewNote) {
        CircleJoinApplicationPO po = checkPending(id);
        po.setStatus(CircleApplicationConstants.STATUS_APPROVED);
        po.setReviewerId(reviewerId);
        po.setReviewNote(reviewNote);
        po.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(po);
        // 同模块内同步加入圈子，事务保证一致性
        circleService.joinCircle(po.getApplicantId(), po.getCircleId());
        // 发布审计事件：入圈申请审核通过
        try {
            AuditEvent auditEvent = AuditEvent.builder()
                    .operatorId(reviewerId)
                    .operatorName(null)
                    .action(AuditConstants.ACTION_CIRCLE_JOIN_APPROVE)
                    .targetType("CIRCLE_JOIN_APPLICATION")
                    .targetId(id)
                    .detail(null)
                    .ip(null)
                    .createTime(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(AuditConstants.EXCHANGE, AuditConstants.ROUTING_KEY, auditEvent);
        } catch (Exception e) {
            log.error("发布审计事件失败: action={}, targetId={}", AuditConstants.ACTION_CIRCLE_JOIN_APPROVE, id, e);
        }
        log.info("入圈申请审核通过并加入圈子完成: applicationId={}, reviewerId={}, circleId={}, applicantId={}",
                id, reviewerId, po.getCircleId(), po.getApplicantId());
    }

    /**
     * 驳回入圈申请
     *
     * @param id          申请记录 ID
     * @param reviewerId  审核人用户 ID
     * @param reviewNote  审核意见
     */
    @Override
    public void rejectApplication(Long id, Long reviewerId, String reviewNote) {
        CircleJoinApplicationPO po = checkPending(id);
        po.setStatus(CircleApplicationConstants.STATUS_REJECTED);
        po.setReviewerId(reviewerId);
        po.setReviewNote(reviewNote);
        po.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(po);
        // 发布审计事件：入圈申请审核驳回
        try {
            AuditEvent auditEvent = AuditEvent.builder()
                    .operatorId(reviewerId)
                    .operatorName(null)
                    .action(AuditConstants.ACTION_CIRCLE_JOIN_REJECT)
                    .targetType("CIRCLE_JOIN_APPLICATION")
                    .targetId(id)
                    .detail(null)
                    .ip(null)
                    .createTime(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(AuditConstants.EXCHANGE, AuditConstants.ROUTING_KEY, auditEvent);
        } catch (Exception e) {
            log.error("发布审计事件失败: action={}, targetId={}", AuditConstants.ACTION_CIRCLE_JOIN_REJECT, id, e);
        }
        log.info("入圈申请审核驳回: applicationId={}, reviewerId={}", id, reviewerId);
    }

    /**
     * 校验申请记录存在且仍为 PENDING 状态
     */
    private CircleJoinApplicationPO checkPending(Long id) {
        CircleJoinApplicationPO po = applicationMapper.selectById(id);
        if (po == null) {
            throw new BusinessException(ErrorCode.CIRCLE_JOIN_APPLICATION_NOT_FOUND);
        }
        if (!CircleApplicationConstants.STATUS_PENDING.equals(po.getStatus())) {
            throw new BusinessException(ErrorCode.CIRCLE_JOIN_APPLICATION_ALREADY_HANDLED);
        }
        return po;
    }

    /**
     * PO → VO 转换
     */
    private CircleJoinApplicationVO toVO(CircleJoinApplicationPO po) {
        CircleJoinApplicationVO vo = new CircleJoinApplicationVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }
}
