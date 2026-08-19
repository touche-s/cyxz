package com.cyxz.post.task;

import com.cyxz.common.utils.RedisLockUtil;
import com.cyxz.post.service.PostCountFlushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCountFlushTask {

    private static final String LOCK_KEY = "lock:count-flush:post";

    private final PostCountFlushService flushService;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(fixedDelay = 30_000)
    public void flushAll() {
        // 多实例互斥：避免多副本同时刷库互相覆盖增量
        String lockToken = RedisLockUtil.tryLock(stringRedisTemplate, LOCK_KEY, 60);
        if (lockToken == null) {
            log.debug("post 计数刷库任务被其他实例持有锁，跳过本次执行");
            return;
        }
        try {
            flushService.flushViewCounts();
            flushService.flushLikeCounts();
            flushService.flushCollectCounts();
            flushService.flushCommentCounts();
        } finally {
            RedisLockUtil.unlock(stringRedisTemplate, LOCK_KEY, lockToken);
        }
    }
}
