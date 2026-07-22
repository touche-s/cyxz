package com.cyxz.comment.service.impl;

import com.cyxz.comment.mapper.CommentMapper;
import com.cyxz.comment.service.CommentCountFlushService;
import com.cyxz.common.constant.CacheKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 评论计数刷库服务实现
 * <p>定时将 Redis Hash 评论点赞增量刷入 MySQL comment.likes。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentCountFlushServiceImpl implements CommentCountFlushService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CommentMapper commentMapper;

    /**
     * 刷评论点赞增量到 comment.likes
     */
    @Override
    public int flushLikeCounts() {
        return flushDelta(CacheKeyConstants.COMMENT_LIKE_DELTA, commentMapper::updateLikes);
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
