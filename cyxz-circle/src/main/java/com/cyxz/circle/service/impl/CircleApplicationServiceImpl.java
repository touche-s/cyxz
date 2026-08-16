package com.cyxz.circle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.audit.api.constant.AuditConstants;
import com.cyxz.audit.api.event.AuditEvent;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.circle.constant.CircleApplicationConstants;
import com.cyxz.circle.dto.CreateCircleApplicationRequest;
import com.cyxz.circle.entity.CircleApplicationPO;
import com.cyxz.circle.mapper.CircleApplicationMapper;
import com.cyxz.circle.service.CircleApplicationService;
import com.cyxz.circle.service.CircleService;
import com.cyxz.circle.vo.CircleApplicationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 圈子创建申请服务实现
 * <p>负责建圈申请的提交、查询与审核。审核通过时在同模块内直接调用
 * {@link CircleService#createCircle(String, String, String, String, Long)} 完成建圈，
 * 借助同一 MySQL 实例的本地事务保证一致性，不再依赖跨模块 MQ 事件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircleApplicationServiceImpl implements CircleApplicationService {

    private final CircleApplicationMapper applicationMapper;
    private final CircleService circleService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 提交建圈申请
     * <p>同一用户存在待审核申请时拒绝重复提交，状态机限定为 PENDING。
     *
     * @param applicantId 申请人用户 ID
     * @param request     建圈申请请求（name / intro / avatar / cover）
     * @return 申请记录 ID
     */
    @Override
    public Long createApplication(Long applicantId, CreateCircleApplicationRequest request) {
        Long exists = applicationMapper.selectCount(new LambdaQueryWrapper<CircleApplicationPO>()
                .eq(CircleApplicationPO::getApplicantId, applicantId)
                .eq(CircleApplicationPO::getStatus, CircleApplicationConstants.STATUS_PENDING));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CIRCLE_APPLICATION_DUPLICATE);
        }
        CircleApplicationPO po = new CircleApplicationPO();
        po.setApplicantId(applicantId);
        po.setName(request.getName());
        po.setIntro(request.getIntro());
        po.setAvatar(request.getAvatar());
        po.setCover(request.getCover());
        po.setStatus(CircleApplicationConstants.STATUS_PENDING);
        applicationMapper.insert(po);
        log.info("用户提交建圈申请: applicationId={}, applicantId={}, name={}",
                po.getId(), applicantId, request.getName());
        return po.getId();
    }

    /**
     * 查询申请人的建圈申请列表，按提交时间倒序分页
     *
     * @param applicantId 申请人用户 ID
     * @param page        页码
     * @param size        每页条数
     * @return 申请记录分页
     */
    @Override
    public PageResult<CircleApplicationVO> listByApplicant(Long applicantId, int page, int size) {
        LambdaQueryWrapper<CircleApplicationPO> wrapper = new LambdaQueryWrapper<CircleApplicationPO>()
                .eq(CircleApplicationPO::getApplicantId, applicantId)
                .orderByDesc(CircleApplicationPO::getCreateTime);
        Page<CircleApplicationPO> p = applicationMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords().stream().map(this::toVO).toList(), p.getTotal(), page, size);
    }

    /**
     * 管理端建圈申请列表，支持按状态筛选并分页
     *
     * @param status 状态筛选（null=全部）
     * @param page   页码
     * @param size   每页条数
     * @return 申请记录分页
     */
    @Override
    public PageResult<CircleApplicationVO> listForAdmin(String status, int page, int size) {
        LambdaQueryWrapper<CircleApplicationPO> wrapper = new LambdaQueryWrapper<CircleApplicationPO>()
                .eq(StringUtils.hasText(status), CircleApplicationPO::getStatus, status)
                .orderByDesc(CircleApplicationPO::getCreateTime);
        Page<CircleApplicationPO> p = applicationMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords().stream().map(this::toVO).toList(), p.getTotal(), page, size);
    }

    /**
     * 查询建圈申请详情
     *
     * @param id 申请记录 ID
     * @return 申请详情 VO
     */
    @Override
    public CircleApplicationVO getDetail(Long id) {
        CircleApplicationPO po = applicationMapper.selectById(id);
        if (po == null) {
            throw new BusinessException(ErrorCode.CIRCLE_APPLICATION_NOT_FOUND);
        }
        return toVO(po);
    }

    /**
     * 审核通过建圈申请
     * <p>更新申请状态为 APPROVED 后，同事务内直接调用
     * {@link CircleService#createCircle(String, String, String, String, Long)} 创建圈子，
     * 从而保证「审核通过」与「圈子建立」的本地一致性。
     *
     * @param id          申请记录 ID
     * @param reviewerId  审核人用户 ID
     * @param reviewNote  审核意见
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveApplication(Long id, Long reviewerId, String reviewNote) {
        CircleApplicationPO po = checkPending(id);
        po.setStatus(CircleApplicationConstants.STATUS_APPROVED);
        po.setReviewerId(reviewerId);
        po.setReviewNote(reviewNote);
        po.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(po);
        // 同模块内同步建圈，事务保证一致性
        circleService.createCircle(po.getName(), po.getIntro(), po.getAvatar(), po.getCover(), po.getApplicantId());
        // 发布审计事件：建圈申请审核通过
        try {
            AuditEvent auditEvent = AuditEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .operatorId(reviewerId)
                    .operatorName(null)
                    .action(AuditConstants.ACTION_CIRCLE_APPROVE)
                    .targetType("CIRCLE_APPLICATION")
                    .targetId(id)
                    .detail(null)
                    .ip(null)
                    .createTime(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(AuditConstants.EXCHANGE, AuditConstants.ROUTING_KEY, auditEvent);
        } catch (Exception e) {
            log.error("发布审计事件失败: action={}, targetId={}", AuditConstants.ACTION_CIRCLE_APPROVE, id, e);
        }
        log.info("建圈申请审核通过并建圈完成: applicationId={}, reviewerId={}, name={}",
                id, reviewerId, po.getName());
    }

    /**
     * 驳回建圈申请
     *
     * @param id          申请记录 ID
     * @param reviewerId  审核人用户 ID
     * @param reviewNote  审核意见
     */
    @Override
    public void rejectApplication(Long id, Long reviewerId, String reviewNote) {
        CircleApplicationPO po = checkPending(id);
        po.setStatus(CircleApplicationConstants.STATUS_REJECTED);
        po.setReviewerId(reviewerId);
        po.setReviewNote(reviewNote);
        po.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(po);
        // 发布审计事件：建圈申请审核驳回
        try {
            AuditEvent auditEvent = AuditEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .operatorId(reviewerId)
                    .operatorName(null)
                    .action(AuditConstants.ACTION_CIRCLE_REJECT)
                    .targetType("CIRCLE_APPLICATION")
                    .targetId(id)
                    .detail(null)
                    .ip(null)
                    .createTime(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(AuditConstants.EXCHANGE, AuditConstants.ROUTING_KEY, auditEvent);
        } catch (Exception e) {
            log.error("发布审计事件失败: action={}, targetId={}", AuditConstants.ACTION_CIRCLE_REJECT, id, e);
        }
        log.info("建圈申请审核驳回: applicationId={}, reviewerId={}", id, reviewerId);
    }

    /**
     * 校验申请记录存在且仍为 PENDING 状态
     */
    private CircleApplicationPO checkPending(Long id) {
        CircleApplicationPO po = applicationMapper.selectById(id);
        if (po == null) {
            throw new BusinessException(ErrorCode.CIRCLE_APPLICATION_NOT_FOUND);
        }
        if (!CircleApplicationConstants.STATUS_PENDING.equals(po.getStatus())) {
            throw new BusinessException(ErrorCode.CIRCLE_APPLICATION_ALREADY_HANDLED);
        }
        return po;
    }

    /**
     * PO → VO 转换
     */
    private CircleApplicationVO toVO(CircleApplicationPO po) {
        CircleApplicationVO vo = new CircleApplicationVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }
}
