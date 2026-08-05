package com.cyxz.post.service.impl;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.post.constant.PostStatus;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.AiReviewService.AiReviewResult;
import com.cyxz.message.event.NotificationEvent;
import com.cyxz.message.utils.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

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
     */
    public void approvePost(Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        if (po.getStatus() != PostStatus.PENDING) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该帖子不在待审核状态");
        }
        if (!PostStatus.canTransition(po.getStatus(), PostStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不允许从 " + PostStatus.label(po.getStatus()) + " 直接变更为 " + PostStatus.label(PostStatus.APPROVED));
        }
        po.setStatus(PostStatus.APPROVED);
        po.setReviewReason(null);
        postMapper.updateById(po);
        postQueryService.evictDetailCache(postId);
        postEsSyncService.syncPostToEs(po);
        log.info("帖子审核通过: postId={}", postId);
    }

    /**
     * 人工审核拒绝
     */
    public void rejectPost(Long postId, String reason) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        if (!PostStatus.canTransition(po.getStatus(), PostStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不允许从 " + PostStatus.label(po.getStatus()) + " 直接变更为 " + PostStatus.label(PostStatus.REJECTED));
        }
        po.setStatus(PostStatus.REJECTED);
        po.setReviewReason(reason);
        postMapper.updateById(po);
        postQueryService.evictDetailCache(postId);
        log.info("帖子审核拒绝: postId={}, reason={}", postId, reason);
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
            sendReviewNotify(authorId, "POST_APPROVED", null, postId, title);
            log.info("AI 审核通过: postId={}", postId);
        } else {
            po.setStatus(PostStatus.REJECTED);
            po.setReviewReason(result.getReason());
            postMapper.updateById(po);
            postQueryService.evictDetailCache(postId);
            sendReviewNotify(authorId, "POST_REJECTED", result.getReason(), postId, title);
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
        log.warn("AI 审核异常，转人工审核: postId={}, error={}", postId, e.getMessage());
    }

    private void sendReviewNotify(Long receiverId, String type, String reason, Long postId, String title) {
        String content = "POST_APPROVED".equals(type)
                ? "你的帖子《" + title + "》审核通过，已公开发布"
                : "你的帖子《" + title + "》未通过审核" + (reason != null ? "：" + reason : "");
        NotificationEvent event = NotificationEvent.builder()
                .receiverId(receiverId)
                .senderId(0L)
                .type(type)
                .title("审核结果")
                .content(content)
                .targetType("post")
                .targetId(postId)
                .createTime(System.currentTimeMillis())
                .build();
        NotificationPublisher.publish(rabbitTemplate, event);
    }
}
