package com.cyxz.comment.task;

import com.cyxz.comment.service.CommentCountFlushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 评论计数刷库任务
 * <p>定时将 Redis Hash 中评论点赞增量刷入 MySQL comment.likes。
 * <p>刷库逻辑委托给 {@link CommentCountFlushService}，本类只负责调度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCountFlushTask {

    private final CommentCountFlushService flushService;

    /** 每 30 秒执行一次 */
    @Scheduled(fixedDelay = 30_000)
    public void flushAll() {
        flushService.flushLikeCounts();
    }
}
