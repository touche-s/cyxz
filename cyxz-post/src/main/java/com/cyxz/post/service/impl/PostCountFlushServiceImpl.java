package com.cyxz.post.service.impl;

import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 帖子计数刷库服务实现
 * <p>定时将 Redis Hash 中的增量刷入 MySQL post 表。
 * <p>策略：遍历 Hash 所有 field，逐条 update，成功后删除对应 field。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostCountFlushServiceImpl implements com.cyxz.post.service.PostCountFlushService {

    private final StringRedisTemplate stringRedisTemplate;
    private final PostMapper postMapper;

    /**
     * 刷浏览增量到 post.views
     * <p>浏览只增不减，delta <= 0 时跳过。
     */
    @Override
    public int flushViewCounts() {
        return flushDelta(CacheKeyConstants.POST_VIEW_DELTA, (postId, delta) -> {
            if (delta <= 0) return;
            postMapper.updateViews(postId, delta);
        });
    }

    /**
     * 刷点赞增量到 post.likes
     */
    @Override
    public int flushLikeCounts() {
        return flushDelta(CacheKeyConstants.POST_LIKE_DELTA, postMapper::updateLikes);
    }

    /**
     * 刷收藏增量到 post.collections
     */
    @Override
    public int flushCollectCounts() {
        return flushDelta(CacheKeyConstants.POST_COLLECT_DELTA, postMapper::updateCollections);
    }

    /**
     * 刷评论数增量到 post.comments
     */
    @Override
    public int flushCommentCounts() {
        return flushDelta(CacheKeyConstants.POST_COMMENT_DELTA, postMapper::updateComments);
    }

    /**
     * 通用增量刷库逻辑
     * <p>遍历 Hash 所有 field，逐条 update，成功后删除 field。
     * 失败时不删，下次定时任务继续重试。
     */
    private int flushDelta(String deltaKey, DeltaUpdater updater) {
        HashOperations<String, Object, Object> hashOps = stringRedisTemplate.opsForHash();
        Map<Object, Object> deltas = hashOps.entries(deltaKey);
        if (deltas == null || deltas.isEmpty()) {
            return 0;
        }

        int success = 0;
        for (Map.Entry<Object, Object> entry : deltas.entrySet()) {
            String idStr = String.valueOf(entry.getKey());
            String deltaStr = String.valueOf(entry.getValue());
            try {
                Long entityId = Long.valueOf(idStr);
                int delta = Integer.parseInt(deltaStr);
                updater.apply(entityId, delta);
                hashOps.delete(deltaKey, idStr);
                success++;
            } catch (NumberFormatException e) {
                log.warn("增量格式异常，跳过: key={}, id={}, delta={}", deltaKey, idStr, deltaStr);
                hashOps.delete(deltaKey, idStr);
            } catch (Exception e) {
                log.error("刷库失败，保留增量等下次重试: key={}, id={}, delta={}", deltaKey, idStr, deltaStr, e);
            }
        }
        if (success > 0) {
            log.info("刷库完成: key={}, 成功 {} 条", deltaKey, success);
        }
        return success;
    }

    /** 增量更新回调，接收实体 ID 和增量值 */
    @FunctionalInterface
    private interface DeltaUpdater {
        void apply(Long entityId, int delta);
    }
}
