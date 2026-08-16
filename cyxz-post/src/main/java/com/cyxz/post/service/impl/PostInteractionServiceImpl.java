package com.cyxz.post.service.impl;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.post.constant.PostStatus;
import com.cyxz.common.utils.IpUtil;
import com.cyxz.common.utils.TransactionUtils;
import com.cyxz.message.enums.NotificationTargetType;
import com.cyxz.message.enums.NotificationType;
import com.cyxz.message.event.NotificationEvent;
import com.cyxz.message.utils.NotificationPublisher;
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
 * <p>计数方案：关系表照常写，计数通过 Redis Hash 增量记录，由 PostCountFlushTask 定时刷库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostInteractionServiceImpl implements PostInteractionService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCollectMapper postCollectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    // ==================== 点赞 ====================

    /**
     * 点赞帖子（幂等，并发安全）
     * <p>UPSERT 一条 SQL 完成：rows=1 新增(发通知), rows=2 恢复, rows=0 幂等。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long userId, Long postId) {
        PostPO po = requirePublishedPost(postId);
        int rows = postLikeMapper.upsertLike(postId, userId);
        if (rows == 1) {
            incrementLikeDelta(postId, 1);
            syncRelationSet(CacheKeyConstants.getUserLikedPostsKey(userId), postId, true);
            log.debug("点赞帖子: postId={}, userId={}", postId, userId);
            sendLikeNotification(postId, userId, po);
        } else if (rows == 2) {
            incrementLikeDelta(postId, 1);
            syncRelationSet(CacheKeyConstants.getUserLikedPostsKey(userId), postId, true);
            log.debug("点赞帖子(恢复): postId={}, userId={}", postId, userId);
        }
    }

    /**
     * 取消点赞帖子（幂等，并发安全）
     * <p>条件 UPDATE：仅 status=1 时更新为 0，一条 SQL 搞定。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long userId, Long postId) {
        requirePublishedPost(postId);
        int rows = postLikeMapper.deactivateLike(postId, userId);
        if (rows > 0) {
            incrementLikeDelta(postId, -1);
            syncRelationSet(CacheKeyConstants.getUserLikedPostsKey(userId), postId, false);
            log.debug("取消点赞帖子: postId={}, userId={}", postId, userId);
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
        requirePublishedPost(postId);
        int rows = postCollectMapper.upsertCollect(postId, userId);
        if (rows > 0) {
            incrementCollectDelta(postId, 1);
            syncRelationSet(CacheKeyConstants.getUserCollectedPostsKey(userId), postId, true);
            log.debug("收藏帖子{}: postId={}, userId={}", rows == 1 ? "" : "(恢复)", postId, userId);
        }
    }

    /**
     * 取消收藏帖子（幂等，并发安全）
     * <p>条件 UPDATE：仅 status=1 时更新为 0，一条 SQL 搞定。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectPost(Long userId, Long postId) {
        requirePublishedPost(postId);
        int rows = postCollectMapper.deactivateCollect(postId, userId);
        if (rows > 0) {
            incrementCollectDelta(postId, -1);
            syncRelationSet(CacheKeyConstants.getUserCollectedPostsKey(userId), postId, false);
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
        String dedupKey = CacheKeyConstants.getPostViewDedupKey(postId, identity);

        Boolean firstView = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofMinutes(CacheKeyConstants.POST_VIEW_DEDUP_MINUTES));

        if (Boolean.TRUE.equals(firstView)) {
            stringRedisTemplate.opsForHash()
                    .increment(CacheKeyConstants.POST_VIEW_DELTA, postId.toString(), 1);
        }
    }

    // ==================== 校验 ====================

    /**
     * 校验帖子存在且已发布，否则拒绝互动操作
     *
     * @param postId 帖子 ID
     * @return 已发布的帖子实体
     */
    private PostPO requirePublishedPost(Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null || po.getStatus() != PostStatus.APPROVED) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        return po;
    }

    // ==================== Redis Delta ====================

    /**
     * 注册事务提交后的点赞增量，避免事务回滚后 Redis 残留脏增量被刷库任务写入
     */
    private void incrementLikeDelta(Long postId, int delta) {
        TransactionUtils.afterCommit(() -> {
            try {
                stringRedisTemplate.opsForHash()
                        .increment(CacheKeyConstants.POST_LIKE_DELTA, postId.toString(), delta);
            } catch (Exception e) {
                log.error("点赞增量写入失败，本周期计数可能偏少: postId={}, delta={}", postId, delta, e);
            }
        });
    }

    /**
     * 注册事务提交后的收藏增量，避免事务回滚后 Redis 残留脏增量被刷库任务写入
     */
    private void incrementCollectDelta(Long postId, int delta) {
        TransactionUtils.afterCommit(() -> {
            try {
                stringRedisTemplate.opsForHash()
                        .increment(CacheKeyConstants.POST_COLLECT_DELTA, postId.toString(), delta);
            } catch (Exception e) {
                log.error("收藏增量写入失败，本周期计数可能偏少: postId={}, delta={}", postId, delta, e);
            }
        });
    }

    // ==================== 关系缓存 ====================

    /**
     * 事务提交后同步关系缓存（点赞/收藏 Set）
     * <p>DB 事务提交成功才写 Redis，避免回滚导致缓存脏数据；写失败仅记日志（DB 是事实来源，读路径可兜底重建）。
     *
     * @param key    关系集合 Key
     * @param postId 帖子 ID
     * @param add    true=加入集合(SADD)，false=移除集合(SREM)
     */
    private void syncRelationSet(String key, Long postId, boolean add) {
        TransactionUtils.afterCommit(() -> {
            try {
                if (add) {
                    // 先移除空占位哨兵，再加入真实成员
                    stringRedisTemplate.opsForSet().remove(key, CacheKeyConstants.EMPTY_SET_PLACEHOLDER);
                    stringRedisTemplate.opsForSet().add(key, postId.toString());
                } else {
                    stringRedisTemplate.opsForSet().remove(key, postId.toString());
                }
                stringRedisTemplate.expire(key, Duration.ofDays(CacheKeyConstants.RELATION_CACHE_TTL_DAYS));
            } catch (Exception e) {
                log.warn("同步关系缓存失败: key={}, postId={}, add={}", key, postId, add, e);
            }
        });
    }

    // ==================== 通知辅助方法 ====================

    /**
     * 发送点赞通知
     * <p>通过 MQ 异步发布点赞通知，失败仅记录日志不影响主流程。
     *
     * @param postId 帖子 ID
     * @param userId 点赞用户 ID
     * @param po     帖子实体（用于获取帖子作者作为接收者）
     */
    private void sendLikeNotification(Long postId, Long userId, PostPO po) {
        NotificationEvent event = NotificationPublisher.of(
                po.getUserId(), userId, NotificationType.POST_LIKED,
                "有人赞了你的帖子", NotificationTargetType.POST, postId);
        TransactionUtils.afterCommit(() -> NotificationPublisher.publish(rabbitTemplate, event));
    }

}
