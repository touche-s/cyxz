package com.cyxz.analytics.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;

/**
 * 统计事件消费者
 * <p>消费各业务服务发布的 {@link AnalyticsEvent}，对每日统计表执行 UPSERT：
 * 已存在 (stat_date, metric) 记录则累加 value，否则新建记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventConsumer extends AbstractManualAckRabbitListener<AnalyticsEvent> {

    private final DailyStatisticMapper dailyStatisticMapper;

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
        LocalDate statDate = event.getStatDate() == null ? LocalDate.now() : event.getStatDate();
        int delta = event.getValue() == null ? 0 : event.getValue();

        DailyStatisticPO existing = dailyStatisticMapper.selectOne(new LambdaQueryWrapper<DailyStatisticPO>()
                .eq(DailyStatisticPO::getStatDate, statDate)
                .eq(DailyStatisticPO::getMetric, event.getMetric()));
        if (existing != null) {
            int current = existing.getValue() == null ? 0 : existing.getValue();
            existing.setValue(current + delta);
            dailyStatisticMapper.updateById(existing);
            log.info("统计指标累加: metric={}, statDate={}, value={}", event.getMetric(), statDate, existing.getValue());
        } else {
            DailyStatisticPO po = new DailyStatisticPO();
            po.setStatDate(statDate);
            po.setMetric(event.getMetric());
            po.setValue(delta);
            dailyStatisticMapper.insert(po);
            log.info("统计指标新建: metric={}, statDate={}, value={}", event.getMetric(), statDate, delta);
        }
    }

    @Override
    protected String describe(AnalyticsEvent event) {
        return "AnalyticsEvent(metric=" + event.getMetric() + ", statDate=" + event.getStatDate() + ")";
    }
}
