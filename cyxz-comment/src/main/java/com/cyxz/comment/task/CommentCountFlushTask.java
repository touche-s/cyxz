package com.cyxz.comment.task;

import com.cyxz.common.utils.RedisLockUtil;
import com.cyxz.comment.service.CommentCountFlushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 评论计数刷库任务
 * <p>定时将 Redis Hash 中评论点赞增量刷入 MySQL comment.likes。
 * <p>刷库逻辑委托给 {@link CommentCountFlushService}，本类只负责调度与多实例互斥。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCountFlushTask {

    private static final String LOCK_KEY = "lock:count-flush:comment";

    private final CommentCountFlushService flushService;
    private final StringRedisTemplate stringRedisTemplate;

    /** 每 30 秒执行一次 */
    @Scheduled(fixedDelay = 30_000)
    public void flushAll() {
        // 多实例互斥：避免多副本同时刷库互相覆盖增量
        if (!RedisLockUtil.tryLock(stringRedisTemplate, LOCK_KEY, 60)) {
            log.debug("comment 计数刷库任务被其他实例持有锁，跳过本次执行");
            return;
        }
        try {
            flushService.flushLikeCounts();
        } finally {
            RedisLockUtil.unlock(stringRedisTemplate, LOCK_KEY);
        }
    }
}
