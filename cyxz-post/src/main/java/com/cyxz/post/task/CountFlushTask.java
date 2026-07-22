package com.cyxz.post.task;

import com.cyxz.post.service.PostCountFlushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 帖子计数刷库任务
 * <p>定时将 Redis Hash 中的各维度增量（浏览/点赞/收藏/评论数）刷入 MySQL post 表。
 * <p>刷库逻辑委托给 {@link PostCountFlushService}，本类只负责调度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountFlushTask {

    private final PostCountFlushService flushService;

    /** 每 30 秒执行一次 */
    @Scheduled(fixedDelay = 30_000)
    public void flushAll() {
        flushService.flushViewCounts();
        flushService.flushLikeCounts();
        flushService.flushCollectCounts();
        flushService.flushCommentCounts();
    }
}
