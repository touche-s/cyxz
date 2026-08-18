package com.cyxz.analytics.consumer;

import com.cyxz.analytics.entity.DailyStatisticPO;
import com.cyxz.analytics.mapper.DailyStatisticMapper;
import com.cyxz.common.constant.AnalyticsConstants;
import com.cyxz.common.consumer.AbstractManualAckRabbitListener;
import com.cyxz.common.event.AnalyticsEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;

/**
 * 统计事件消费者
 * <p>消费各业务服务发布的 {@link AnalyticsEvent}，通过 MySQL {@code ON DUPLICATE KEY UPDATE}
 * 实现原子 UPSERT 累加。注意 UPSERT 只保证并发安全不保证幂等——重复投递会导致计数翻倍，
 * 因此先按 eventId 做 Redis SETNX 防重（24h），与 PostCountConsumer 保持一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventConsumer extends AbstractManualAckRabbitListener<AnalyticsEvent> {

    private final DailyStatisticMapper dailyStatisticMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DEDUP_PREFIX = "analytics:dedup:";
    private static final Duration DEDUP_TTL = Duration.ofHours(24);

    @RabbitListener(queues = AnalyticsConstants.QUEUE, ackMode = "MANUAL")
    public void onEvent(AnalyticsEvent event, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        processWithManualAck(event, channel, tag);
    }

    /**
     * 死信监听：记录日志，不再重试
     */
    @RabbitListener(queues = AnalyticsConstants.DLQ)
    public void onDeadLetter(AnalyticsEvent event) {
        log.error("统计事件死信: metric={}, value={}, statDate={}",
                event.getMetric(), event.getValue(), event.getStatDate());
    }

    @Override
    protected void handle(AnalyticsEvent event) {
        // eventId 防重：SETNX 失败则跳过（旧消息无 eventId 时退化为直接累加）
        if (event.getEventId() != null) {
            Boolean first = stringRedisTemplate.opsForValue()
                    .setIfAbsent(DEDUP_PREFIX + event.getEventId(), "1", DEDUP_TTL);
            if (Boolean.FALSE.equals(first)) {
                log.debug("统计事件重复跳过: eventId={}, metric={}", event.getEventId(), event.getMetric());
                return;
            }
        }
        LocalDate statDate = event.getStatDate() == null ? LocalDate.now() : event.getStatDate();
        int delta = event.getValue() == null ? 0 : event.getValue();

        DailyStatisticPO po = new DailyStatisticPO();
        po.setStatDate(statDate);
        po.setMetric(event.getMetric());
        po.setValue(delta);
        // ON DUPLICATE KEY UPDATE value = value + VALUES(value)，原子累加，幂等由上方 eventId 防重保证
        dailyStatisticMapper.upsert(po);
        log.debug("统计指标 UPSERT: metric={}, statDate={}, delta={}", event.getMetric(), statDate, delta);
    }

    @Override
    protected String describe(AnalyticsEvent event) {
        return "AnalyticsEvent(metric=" + event.getMetric() + ", statDate=" + event.getStatDate() + ")";
    }
}
