package com.cyxz.post.task;

import com.cyxz.post.service.CircleService;
import com.cyxz.post.service.PostCountFlushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 计数刷库任务
 * <p>定时将各维度增量数据刷入 MySQL。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountFlushTask {

    private final PostCountFlushService flushService;
    private final CircleService circleService;

    /** 每 30 秒执行一次 */
    @Scheduled(fixedDelay = 30_000)
    public void flushAll() {
        flushService.flushViewCounts();
        flushService.flushLikeCounts();
        flushService.flushCollectCounts();
        flushService.flushCommentCounts();
    }

    /** 每 2 分钟重算一次圈子统计（post_count / member_count） */
    @Scheduled(fixedDelay = 120_000)
    public void recountCircleStats() {
        circleService.recountStats();
    }
}
