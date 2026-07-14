package com.cyxz.post.task;

import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 浏览量刷库任务
 * <p>定时将 Redis Hash {@code post:view:delta} 中的浏览增量刷到 MySQL post.views。
 * <p>策略：遍历 Hash 所有 field，逐条 update，成功后删除对应 field。
 * 当前量级下逐条 update 足够，未来量大可改为批量 SQL。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountFlushTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final PostMapper postMapper;

    /**
     * 每 30 秒执行一次
     * <p>用 HSCAN 遍历增量，避免大 key 阻塞。
     * 刷库成功后删除 field，保证每条增量只刷一次。
     */
    @Scheduled(fixedDelay = 30_000)
    public void flushViewCounts() {
        HashOperations<String, Object, Object> hashOps = stringRedisTemplate.opsForHash();
        Map<Object, Object> deltas = hashOps.entries(CacheKeyConstants.POST_VIEW_DELTA);
        if (deltas == null || deltas.isEmpty()) {
            return;
        }

        int success = 0;
        for (Map.Entry<Object, Object> entry : deltas.entrySet()) {
            String postIdStr = String.valueOf(entry.getKey());
            String deltaStr = String.valueOf(entry.getValue());
            try {
                Long postId = Long.valueOf(postIdStr);
                int delta = Integer.parseInt(deltaStr);
                if (delta <= 0) {
                    // 异常增量，直接清理
                    hashOps.delete(CacheKeyConstants.POST_VIEW_DELTA, postIdStr);
                    continue;
                }
                postMapper.updateViews(postId, delta);
                // 刷库成功后删除增量，避免重复累加
                hashOps.delete(CacheKeyConstants.POST_VIEW_DELTA, postIdStr);
                success++;
            } catch (NumberFormatException e) {
                log.warn("浏览增量格式异常，跳过: postId={}, delta={}", postIdStr, deltaStr);
                hashOps.delete(CacheKeyConstants.POST_VIEW_DELTA, postIdStr);
            } catch (Exception e) {
                log.error("刷库失败，保留增量等下次重试: postId={}, delta={}", postIdStr, deltaStr, e);
                // 不删除 field，下次任务继续重试
            }
        }
        if (success > 0) {
            log.info("浏览量刷库完成: 成功 {} 条", success);
        }
    }
}
