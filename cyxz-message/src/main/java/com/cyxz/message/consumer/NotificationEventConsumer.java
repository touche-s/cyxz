package com.cyxz.message.consumer;

import com.cyxz.message.api.event.NotificationEvent;
import com.cyxz.message.service.impl.NotificationServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationServiceImpl notificationService;

    @RabbitListener(queues = "cyxz.notification.queue", ackMode = "MANUAL")
    public void onEvent(NotificationEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            boolean ok = notificationService.createByEvent(event);
            if (ok) {
                log.info("事件消费成功: type={}, receiverId={}", event.getType(), event.getReceiverId());
            }
        } catch (Exception e) {
            log.error("事件消费失败，进入死信: type={}", event.getType(), e);
            channel.basicReject(tag, false);
            return;
        }
        channel.basicAck(tag, false);
    }
}
