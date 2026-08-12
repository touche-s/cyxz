package com.cyxz.message.consumer;

import com.cyxz.common.consumer.AbstractManualAckRabbitListener;
import com.cyxz.message.constant.NotificationConstants;
import com.cyxz.message.event.NotificationEvent;
import com.cyxz.message.service.impl.NotificationServiceImpl;
import com.cyxz.message.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer extends AbstractManualAckRabbitListener<NotificationEvent> {

    private final NotificationServiceImpl notificationService;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = NotificationConstants.QUEUE, ackMode = "MANUAL")
    public void onEvent(NotificationEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag, Message message) throws IOException {
        processWithManualAck(event, channel, tag, message);
    }

    @Override
    protected void handle(NotificationEvent event) throws Exception {
        boolean ok = notificationService.createByEvent(event);
        if (ok) {
            log.info("事件消费成功: type={}, receiverId={}", event.getType(), event.getReceiverId());
            pushIfOnline(event);
        }
    }

    @Override
    protected String describe(NotificationEvent event) {
        return "type=" + event.getType() + ", receiverId=" + event.getReceiverId();
    }

    /**
     * 通知落库后，通过 WebSocket 实时推送给在线用户
     */
    private void pushIfOnline(NotificationEvent event) {
        if (!sessionManager.isOnline(event.getReceiverId())) {
            return;
        }
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("type", "notification");
            envelope.put("data", event);
            sessionManager.sendToUser(event.getReceiverId(), objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.warn("通知 WS 推送失败: receiverId={}", event.getReceiverId(), e);
        }
    }
}
