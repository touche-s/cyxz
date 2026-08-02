package com.cyxz.post.service.impl;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.PostStatus;
import com.cyxz.common.utils.IpUtil;
import com.cyxz.message.api.dto.CreateNotificationRequest;
import com.cyxz.message.api.enums.NotificationType;
import com.cyxz.message.api.constant.NotificationConstants;
import com.cyxz.message.api.event.NotificationEvent;
import com.cyxz.message.api.feign.MessageFeignClient;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostCollectMapper;
import com.cyxz.post.mapper.PostLikeMapper;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.PostInteractionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 帖子互动服务实现
 * <p>管理点赞、收藏、浏览等互动操作。
 * <p>计数方案：关系表照常写，计数通过 Redis Hash 增量记录，由 CountFlushTask 定时刷库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostInteractionServiceImpl implements PostInteractionService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCollectMapper postCollectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MessageFeignClient messageFeignClient;
    private final RabbitTemplate rabbitTemplate;

    /** 帖子是否允许互动（仅已发布 PostStatus.APPROVED） */
    private boolean isInteractable(PostPO po) {
        return po != null && po.getStatus() == PostStatus.APPROVED;
    }

    // ==================== 点赞 ====================

    /**
     * 点赞帖子（幂等，并发安全）
     * <p>UPSERT 一条 SQL 完成：rows=1 新增(发通知), rows=2 恢复, rows=0 幂等。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || !isInteractable(po)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        int rows = postLikeMapper.upsertLike(postId, userId);
        if (rows == 1) {
            incrementLikeDelta(postId, 1);
            log.info("点赞帖子: postId={}, userId={}", postId, userId);
            sendLikeNotification(postId, userId, po);
        } else if (rows == 2) {
            incrementLikeDelta(postId, 1);
            log.info("点赞帖子(恢复): postId={}, userId={}", postId, userId);
        }
    }

    /**
     * 取消点赞帖子（幂等，并发安全）
     * <p>条件 UPDATE：仅 status=1 时更新为 0，一条 SQL 搞定。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || !isInteractable(po)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        int rows = postLikeMapper.deactivateLike(postId, userId);
        if (rows > 0) {
            incrementLikeDelta(postId, -1);
            log.info("取消点赞帖子: postId={}, userId={}", postId, userId);
        }
    }

    // ==================== 收藏 ====================

    /**
     * 收藏帖子（幂等，并发安全）
     * <p>UPSERT 一条 SQL 完成：rows=1 新增, rows=2 恢复, rows=0 幂等。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectPost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || !isInteractable(po)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        int rows = postCollectMapper.upsertCollect(postId, userId);
        if (rows > 0) {
            incrementCollectDelta(postId, 1);
            log.info("收藏帖子{}: postId={}, userId={}", rows == 1 ? "" : "(恢复)", postId, userId);
        }
    }

    /**
     * 取消收藏帖子（幂等，并发安全）
     * <p>条件 UPDATE：仅 status=1 时更新为 0，一条 SQL 搞定。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectPost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || !isInteractable(po)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        int rows = postCollectMapper.deactivateCollect(postId, userId);
        if (rows > 0) {
            incrementCollectDelta(postId, -1);
            log.info("取消收藏帖子: postId={}, userId={}", postId, userId);
        }
    }

    // ==================== 浏览 ====================

    /**
     * 记录浏览
     * <p>Redis 去重（30min 内同一用户/IP 只算一次），去重通过则 Hash 增量 +1。
     */
    @Override
    public void recordView(Long postId, Long userId, HttpServletRequest request) {
        PostPO po = postMapper.selectById(postId);
        // 已发布(APPROVED)才计浏览量
        if (po == null || po.getStatus() != PostStatus.APPROVED) {
            return;
        }

        String identity = (userId != null) ? "user:" + userId : "ip:" + IpUtil.getClientIp(request);
        String dedupKey = CacheKeyConstants.POST_VIEW_DEDUP_PREFIX + postId + ":" + identity;

        Boolean firstView = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofMinutes(CacheKeyConstants.POST_VIEW_DEDUP_MINUTES));

        if (Boolean.TRUE.equals(firstView)) {
            stringRedisTemplate.opsForHash()
                    .increment(CacheKeyConstants.POST_VIEW_DELTA, postId.toString(), 1);
        }
    }

    // ==================== Redis Delta ====================

    private void incrementLikeDelta(Long postId, int delta) {
        stringRedisTemplate.opsForHash()
                .increment(CacheKeyConstants.POST_LIKE_DELTA, postId.toString(), delta);
    }

    private void incrementCollectDelta(Long postId, int delta) {
        stringRedisTemplate.opsForHash()
                .increment(CacheKeyConstants.POST_COLLECT_DELTA, postId.toString(), delta);
    }

    // ==================== 通知辅助方法 ====================

    /**
     * 发送点赞通知
     * <p>通过 Feign 调用消息服务创建点赞通知，失败仅记录日志不影响主流程。
     *
     * @param postId 帖子 ID
     * @param userId 点赞用户 ID
     * @param po     帖子实体（用于获取帖子作者作为接收者）
     */
    private void sendLikeNotification(Long postId, Long userId, PostPO po) {
        try {
            rabbitTemplate.convertAndSend(
                NotificationConstants.EXCHANGE,
                NotificationConstants.ROUTING_KEY,
                NotificationEvent.builder()
                    .receiverId(po.getUserId())
                    .senderId(userId)
                    .type(NotificationType.POST_LIKED.name())
                    .title("有人赞了你的帖子")
                    .targetType("post")
                    .targetId(postId)
                    .createTime(System.currentTimeMillis())
                    .build()
            );
        } catch (Exception e) {
            log.warn("MQ 发布点赞通知失败: postId={}, userId={}", postId, userId, e);
        }
    }

}