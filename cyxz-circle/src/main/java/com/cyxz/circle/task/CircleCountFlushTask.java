package com.cyxz.circle.task;

import com.cyxz.circle.service.CircleCountFlushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 圈子计数定时校验：每 60 秒从 post 服务拉取已发布帖子数，覆盖写入 circle.post_count
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CircleCountFlushTask {

    private final CircleCountFlushService flushService;

    @Scheduled(fixedDelay = 60_000)
    public void flushAll() {
        flushService.flushPostCounts();
    }
}
