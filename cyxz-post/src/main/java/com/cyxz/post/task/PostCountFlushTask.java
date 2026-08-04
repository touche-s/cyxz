package com.cyxz.post.task;

import com.cyxz.post.service.PostCountFlushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCountFlushTask {

    private final PostCountFlushService flushService;

    @Scheduled(fixedDelay = 30_000)
    public void flushAll() {
        flushService.flushViewCounts();
        flushService.flushLikeCounts();
        flushService.flushCollectCounts();
        flushService.flushCommentCounts();
    }
}
