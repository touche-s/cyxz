package com.cyxz.message.utils;

import com.cyxz.message.constant.NotificationConstants;
import com.cyxz.message.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 通知事件发布工具
 * <p>封装 RabbitMQ 通知发布的 try/catch 模板，失败仅记录日志不抛异常，避免阻塞业务主流程。
 * <p>调用方注入 {@link RabbitTemplate} 后调用 {@link #publish}。
 */
@Slf4j
public final class NotificationPublisher {

    private NotificationPublisher() {}

    /**
     * 发布通知事件到 MQ，失败仅记录日志不抛异常
     *
     * @param rabbitTemplate RabbitTemplate 实例
     * @param event          通知事件
     */
    public static void publish(RabbitTemplate rabbitTemplate, NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                NotificationConstants.EXCHANGE,
                NotificationConstants.ROUTING_KEY,
                event
            );
        } catch (Exception e) {
            log.warn("MQ 发布通知失败: type={}, receiverId={}", event.getType(), event.getReceiverId(), e);
        }
    }
}
