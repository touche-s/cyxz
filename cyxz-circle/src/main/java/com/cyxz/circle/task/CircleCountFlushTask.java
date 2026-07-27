package com.cyxz.circle.task;

import com.cyxz.circle.service.CircleCountFlushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CircleCountFlushTask {

    private final CircleCountFlushService flushService;

    @Scheduled(fixedDelay = 30_000)
    public void flushAll() {
        flushService.flushPostCounts();
    }
}
