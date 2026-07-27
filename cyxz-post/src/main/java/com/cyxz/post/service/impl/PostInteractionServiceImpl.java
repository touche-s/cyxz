package com.cyxz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.common.utils.IpUtil;
import com.cyxz.common.utils.StatusUpdateHelper;
import com.cyxz.message.api.dto.CreateNotificationRequest;
import com.cyxz.message.api.enums.NotificationType;
import com.cyxz.message.api.event.NotificationEvent;
import com.cyxz.message.api.feign.MessageFeignClient;
import com.cyxz.post.entity.PostCollectPO;
import com.cyxz.post.entity.PostLikePO;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostCollectMapper;
import com.cyxz.post.mapper.PostLikeMapper;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.PostInteractionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
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

    /** 帖子是否允许互动（仅已发布） */
    private boolean isInteractable(PostPO po) {
        return po != null && po.getStatus() == 1;
    }

    // ==================== 点赞 ====================

    /**
     * 点赞帖子（幂等，并发安全）
     * <p>先查关系表，不存在则插入并记增量；已存在且状态为删除则恢复。
     * 并发下 DuplicateKeyException 回退到 CAS 更新状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || !isInteractable(po)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        PostLikePO exist = queryPostLike(userId, postId);

        if (exist == null) {
            try {
                PostLikePO newLike = new PostLikePO();
                newLike.setPostId(postId);
                newLike.setUserId(userId);
                newLike.setStatus(CommonStatus.ACTIVE);
                postLikeMapper.insert(newLike);
                incrementLikeDelta(postId, 1);
                log.info("点赞帖子: postId={}, userId={}", postId, userId);
                sendLikeNotification(postId, userId, po);
            } catch (DuplicateKeyException e) {
                PostLikePO conflict = queryPostLike(userId, postId);
                if (conflict.getStatus() == 1) {
                    return;
                }
                boolean updated = StatusUpdateHelper.updateStatus(postLikeMapper, conflict.getId(), 0, 1);
                if (updated) {
                    incrementLikeDelta(postId, 1);
                    log.info("点赞帖子(并发恢复): postId={}, userId={}", postId, userId);
                }
            }
            return;
        }

        if (exist.getStatus() == 0) {
            boolean updated = StatusUpdateHelper.updateStatus(postLikeMapper, exist.getId(), 0, 1);
            if (updated) {
                incrementLikeDelta(postId, 1);
                log.info("点赞帖子(恢复): postId={}, userId={}", postId, userId);
            }
            return;
        }

        log.debug("点赞帖子(幂等忽略): postId={}, userId={}", postId, userId);
    }

    /**
     * 取消点赞帖子（幂等，并发安全）
     * <p>CAS 将关系表状态 1→0，成功则记 -1 增量。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || !isInteractable(po)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        PostLikePO exist = queryPostLike(userId, postId);
        if (exist == null || exist.getStatus() == 0) {
            return;
        }

        boolean updated = StatusUpdateHelper.updateStatus(postLikeMapper, exist.getId(), 1, 0);
        if (updated) {
            incrementLikeDelta(postId, -1);
            log.info("取消点赞帖子: postId={}, userId={}", postId, userId);
        }
    }

    // ==================== 收藏 ====================

    /**
     * 收藏帖子（幂等，并发安全）
     * <p>逻辑同点赞：先查 → 不存在插入 → 冲突 CAS 恢复。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectPost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || !isInteractable(po)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        PostCollectPO exist = queryPostCollect(userId, postId);

        if (exist == null) {
            try {
                PostCollectPO newCollect = new PostCollectPO();
                newCollect.setPostId(postId);
                newCollect.setUserId(userId);
                newCollect.setStatus(CommonStatus.ACTIVE);
                postCollectMapper.insert(newCollect);
                incrementCollectDelta(postId, 1);
                log.info("收藏帖子: postId={}, userId={}", postId, userId);
            } catch (DuplicateKeyException e) {
                PostCollectPO conflict = queryPostCollect(userId, postId);
                if (conflict.getStatus() == 1) {
                    return;
                }
                boolean updated = StatusUpdateHelper.updateStatus(postCollectMapper, conflict.getId(), 0, 1);
                if (updated) {
                    incrementCollectDelta(postId, 1);
                    log.info("收藏帖子(并发恢复): postId={}, userId={}", postId, userId);
                }
            }
            return;
        }

        if (exist.getStatus() == 0) {
            boolean updated = StatusUpdateHelper.updateStatus(postCollectMapper, exist.getId(), 0, 1);
            if (updated) {
                incrementCollectDelta(postId, 1);
                log.info("收藏帖子(恢复): postId={}, userId={}", postId, userId);
            }
            return;
        }

        log.debug("收藏帖子(幂等忽略): postId={}, userId={}", postId, userId);
    }

    /**
     * 取消收藏帖子（幂等，并发安全）
     * <p>CAS 将关系表状态 1→0，成功则记 -1 增量。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectPost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || !isInteractable(po)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        PostCollectPO exist = queryPostCollect(userId, postId);
        if (exist == null || exist.getStatus() == 0) {
            return;
        }

        boolean updated = StatusUpdateHelper.updateStatus(postCollectMapper, exist.getId(), 1, 0);
        if (updated) {
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
        if (po == null || po.getStatus() != 1) {
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

    // ==================== 辅助查询 ====================

    private PostLikePO queryPostLike(Long userId, Long postId) {
        LambdaQueryWrapper<PostLikePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLikePO::getUserId, userId)
                .eq(PostLikePO::getPostId, postId);
        return postLikeMapper.selectOne(wrapper);
    }

    private PostCollectPO queryPostCollect(Long userId, Long postId) {
        LambdaQueryWrapper<PostCollectPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostCollectPO::getUserId, userId)
                .eq(PostCollectPO::getPostId, postId);
        return postCollectMapper.selectOne(wrapper);
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
                "cyxz.notification.exchange",
                "notification.create",
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