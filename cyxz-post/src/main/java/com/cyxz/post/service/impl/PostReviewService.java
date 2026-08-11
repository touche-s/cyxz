package com.cyxz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyxz.audit.api.constant.AuditConstants;
import com.cyxz.audit.api.event.AuditEvent;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.AnalyticsConstants;
import com.cyxz.common.constant.PostCountConstants;
import com.cyxz.common.event.AnalyticsEvent;
import com.cyxz.post.constant.PostStatus;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.AiReviewService.AiReviewResult;
import com.cyxz.message.enums.NotificationTargetType;
import com.cyxz.message.enums.NotificationType;
import com.cyxz.message.event.NotificationEvent;
import com.cyxz.message.utils.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 帖子审核服务
 * <p>处理人工审核与 AI 审核结果，包含状态流转、缓存清理、ES 同步与通知发送。
 * handleReviewResult/handleReviewFailure 对同包 CommandService 开放，供异步 AI 审核回调。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostReviewService {

    private final PostMapper postMapper;
    private final RabbitTemplate rabbitTemplate;
    private final PostEsSyncService postEsSyncService;
    private final PostQueryService postQueryService;

    /**
     * 人工审核通过
     * <p>使用 CAS 更新防止并发重复审核：仅当原状态为 PENDING 时才对 status 做原子写，
     * rows=0 表示已被其他审核员并发处理，抛出状态冲突异常要求刷新。
     */
    public void approvePost(Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        if (!PostStatus.canTransition(po.getStatus(), PostStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不允许从 " + PostStatus.label(po.getStatus()) + " 直接变更为 " + PostStatus.label(PostStatus.APPROVED));
        }
        int from = po.getStatus();

        // CAS 更新：仅 PENDING → APPROVED 原子写，防并发重复审核
        LambdaUpdateWrapper<PostPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PostPO::getId, postId)
               .eq(PostPO::getStatus, PostStatus.PENDING);
        PostPO update = new PostPO();
        update.setStatus(PostStatus.APPROVED);
        update.setReviewReason(null);
        int rows = postMapper.update(update, wrapper);
        if (rows == 0) {
            log.warn("帖子审核CAS失败(已被并发处理): postId={}", postId);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该帖子已被处理，请刷新列表");
        }
        // 补全 po 的状态供后续事件使用
        po.setStatus(PostStatus.APPROVED);
        po.setReviewReason(null);
        postQueryService.evictDetailCache(postId);
        postEsSyncService.syncPostToEs(po);
        postEsSyncService.publishCountEvent(po, PostCountConstants.ACTION_PUBLISH);
        // 发布审计事件：帖子审核通过（操作人为系统）
        try {
            AuditEvent auditEvent = AuditEvent.builder()
                    .operatorId(0L)
                    .operatorName(null)
                    .action(AuditConstants.ACTION_POST_APPROVE)
                    .targetType("POST")
                    .targetId(postId)
                    .detail(null)
                    .ip(null)
                    .createTime(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(AuditConstants.EXCHANGE, AuditConstants.ROUTING_KEY, auditEvent);
        } catch (Exception e) {
            log.error("发布审计事件失败: action={}, targetId={}", AuditConstants.ACTION_POST_APPROVE, postId, e);
        }
        // 发布统计事件：帖子审核通过数 +1
        try {
            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .metric(AnalyticsConstants.METRIC_POST_APPROVED)
                    .value(1)
                    .statDate(LocalDate.now())
                    .build();
            rabbitTemplate.convertAndSend(AnalyticsConstants.EXCHANGE, AnalyticsConstants.ROUTING_KEY, analyticsEvent);
        } catch (Exception e) {
            log.error("发布统计事件失败: metric={}", AnalyticsConstants.METRIC_POST_APPROVED, e);
        }
        log.info("帖子审核通过: postId={}, {}({})→{}({})", postId,
                PostStatus.label(from), from, PostStatus.label(PostStatus.APPROVED), PostStatus.APPROVED);
    }

    /**
     * 人工审核拒绝
     * <p>使用 CAS 更新防止并发重复审核。
     */
    public void rejectPost(Long postId, String reason) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        if (!PostStatus.canTransition(po.getStatus(), PostStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不允许从 " + PostStatus.label(po.getStatus()) + " 直接变更为 " + PostStatus.label(PostStatus.REJECTED));
        }
        int from = po.getStatus();

        // CAS 更新：仅 PENDING → REJECTED 原子写，防并发重复审核
        LambdaUpdateWrapper<PostPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PostPO::getId, postId)
               .eq(PostPO::getStatus, PostStatus.PENDING);
        PostPO update = new PostPO();
        update.setStatus(PostStatus.REJECTED);
        update.setReviewReason(reason);
        int rows = postMapper.update(update, wrapper);
        if (rows == 0) {
            log.warn("帖子审核CAS失败(已被并发处理): postId={}", postId);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该帖子已被处理，请刷新列表");
        }
        po.setStatus(PostStatus.REJECTED);
        po.setReviewReason(reason);
        postQueryService.evictDetailCache(postId);
        // 发布审计事件：帖子审核拒绝（操作人为系统）
        try {
            AuditEvent auditEvent = AuditEvent.builder()
                    .operatorId(0L)
                    .operatorName(null)
                    .action(AuditConstants.ACTION_POST_REJECT)
                    .targetType("POST")
                    .targetId(postId)
                    .detail(null)
                    .ip(null)
                    .createTime(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(AuditConstants.EXCHANGE, AuditConstants.ROUTING_KEY, auditEvent);
        } catch (Exception e) {
            log.error("发布审计事件失败: action={}, targetId={}", AuditConstants.ACTION_POST_REJECT, postId, e);
        }
        // 发布统计事件：帖子审核驳回数 +1
        try {
            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .metric(AnalyticsConstants.METRIC_POST_REJECTED)
                    .value(1)
                    .statDate(LocalDate.now())
                    .build();
            rabbitTemplate.convertAndSend(AnalyticsConstants.EXCHANGE, AnalyticsConstants.ROUTING_KEY, analyticsEvent);
        } catch (Exception e) {
            log.error("发布统计事件失败: metric={}", AnalyticsConstants.METRIC_POST_REJECTED, e);
        }
        log.info("帖子审核拒绝: postId={}, {}({})→{}({}), reason={}", postId,
                PostStatus.label(from), from, PostStatus.label(PostStatus.REJECTED), PostStatus.REJECTED, reason);
    }

    /**
     * 处理 AI 审核结果
     * <p>通过：更新状态为已通过，清除缓存，发通知
     * <p>拒绝：更新状态为拒绝并记录原因，清除缓存，发通知
     */
    public void handleReviewResult(Long postId, Long authorId, String title, AiReviewResult result) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || po.getStatus() != PostStatus.PENDING) {
            log.info("帖子 {} 状态已变更，跳过审核结果处理", postId);
            return;
        }
        if (result.isPassed()) {
            po.setStatus(PostStatus.APPROVED);
            po.setReviewReason(null);
            postMapper.updateById(po);
            postQueryService.evictDetailCache(postId);
            postEsSyncService.syncPostToEs(po);
            postEsSyncService.publishCountEvent(po, PostCountConstants.ACTION_PUBLISH);
            sendReviewNotify(authorId, NotificationType.POST_APPROVED, null, postId, title);
            log.info("AI 审核通过: postId={}", postId);
        } else {
            po.setStatus(PostStatus.REJECTED);
            po.setReviewReason(result.getReason());
            postMapper.updateById(po);
            postQueryService.evictDetailCache(postId);
            sendReviewNotify(authorId, NotificationType.POST_REJECTED, result.getReason(), postId, title);
            log.warn("AI 审核拒绝: postId={}, reason={}", postId, result.getReason());
        }
    }

    /**
     * AI 审核异常处理：帖子保持 PENDING，标记原因待人工审核
     * <p>AI 服务异常时不放行也不拒绝，由管理员通过后台 PENDING 列表手动处理。
     */
    public void handleReviewFailure(Long postId, Exception e) {
        try {
            PostPO po = postMapper.selectById(postId);
            if (po != null && po.getStatus() == PostStatus.PENDING) {
                po.setReviewReason("AI审核服务异常，待人工审核");
                postMapper.updateById(po);
                postQueryService.evictDetailCache(postId);
            }
        } catch (Exception ex) {
            log.error("标记人工审核失败: postId={}", postId, ex);
        }
        log.warn("AI 审核异常，转人工审核: postId={}", postId, e);
    }

    private void sendReviewNotify(Long receiverId, NotificationType type, String reason, Long postId, String title) {
        String content = type == NotificationType.POST_APPROVED
                ? "你的帖子《" + title + "》审核通过，已公开发布"
                : "你的帖子《" + title + "》未通过审核" + (reason != null ? "：" + reason : "");
        NotificationEvent event = NotificationPublisher.of(
                receiverId, 0L, type, "审核结果",
                NotificationTargetType.POST, postId, null, content);
        NotificationPublisher.publish(rabbitTemplate, event);
    }
}
