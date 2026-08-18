package com.cyxz.governance.task;

import com.cyxz.governance.api.constant.GovernanceConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 内容处置事件补偿任务
 * <p>举报通过后发送 {@code ContentTakedownEvent} 失败时消息会进入 Redis 补偿队列，
 * 本任务定时重发，避免"举报已通过但内容未下架"的永久不一致。
 * <p>重试仍失败的消息放回队首，留待下次扫描；消息体为 JSON 字符串，
 * 与 {@code post} 服务 {@code es:sync} 失败队列的重发模式保持一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TakedownRetryTask {

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 定时重试内容处置事件
     * <p>每 30 秒扫描一次，单次最多处理 50 条，避免积压时长时间占用线程。
     */
    @Scheduled(fixedDelay = 30_000L, initialDelay = 60_000L)
    public void retryFailedTakedown() {
        int maxRetry = 50;
        int succeeded = 0;
        int failed = 0;
        for (int i = 0; i < maxRetry; i++) {
            String json = stringRedisTemplate.opsForList().rightPop(GovernanceConstants.TAKEDOWN_FAILED_QUEUE_KEY);
            if (json == null) {
                break;
            }
            try {
                rabbitTemplate.convertAndSend(GovernanceConstants.EXCHANGE, GovernanceConstants.ROUTING_KEY, json);
                succeeded++;
            } catch (Exception e) {
                stringRedisTemplate.opsForList().leftPush(GovernanceConstants.TAKEDOWN_FAILED_QUEUE_KEY, json);
                failed++;
                log.warn("内容处置事件重试失败，已放回队列: {}", json, e);
                break;
            }
        }
        if (succeeded > 0 || failed > 0) {
            log.info("内容处置事件失败队列重试完成: succeeded={}, failed={}", succeeded, failed);
        }
    }
}
