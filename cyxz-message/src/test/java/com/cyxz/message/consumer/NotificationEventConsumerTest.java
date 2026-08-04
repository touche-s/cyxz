package com.cyxz.message.consumer;

import com.cyxz.message.event.NotificationEvent;
import com.cyxz.message.entity.NotificationPO;
import com.cyxz.message.mapper.NotificationMapper;
import com.cyxz.message.service.impl.NotificationServiceImpl;
import com.cyxz.message.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventConsumer MQ 消费者")
class NotificationEventConsumerTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private Channel channel;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private ObjectMapper objectMapper;

    private NotificationEventConsumer consumer;

    private final long tag = 1L;

    @BeforeEach
    void setUp() {
        NotificationServiceImpl notificationService = new NotificationServiceImpl(notificationMapper, null);
        consumer = new NotificationEventConsumer(notificationService, sessionManager, objectMapper);
    }

    @Nested
    @DisplayName("正常消费")
    class NormalConsume {

        @Test
        @DisplayName("正常事件成功落库并 ACK")
        void shouldAckOnSuccess() throws IOException {
            NotificationEvent event = NotificationEvent.builder()
                    .receiverId(100L)
                    .senderId(200L)
                    .type("COMMENT_REPLIED")
                    .title("有人回复了你的评论")
                    .content("回复内容")
                    .targetType("comment")
                    .targetId(10L)
                    .createTime(System.currentTimeMillis())
                    .build();

            consumer.onEvent(event, channel, tag);

            verify(notificationMapper).insert(any(NotificationPO.class));
            verify(channel).basicAck(eq(tag), eq(false));
        }
    }

    @Nested
    @DisplayName("幂等去重")
    class Idempotency {

        @Test
        @DisplayName("唯一索引冲突（DuplicateKeyException）时跳过落库并 ACK")
        void shouldSkipDuplicateEventAndAck() throws IOException {
            when(notificationMapper.insert(any(NotificationPO.class)))
                    .thenThrow(new DuplicateKeyException("Duplicate entry for uk_dedup"));

            NotificationEvent event = NotificationEvent.builder()
                    .receiverId(100L)
                    .senderId(200L)
                    .type("COMMENT_REPLIED")
                    .title("重复事件")
                    .createTime(System.currentTimeMillis())
                    .build();

            consumer.onEvent(event, channel, tag);

            verify(channel).basicAck(eq(tag), eq(false));
        }
    }

    @Nested
    @DisplayName("消费失败")
    class ConsumeFailure {

        @Test
        @DisplayName("DB 异常时 reject 不 requeue，进入死信")
        void shouldRejectOnDbFailure() throws IOException {
            when(notificationMapper.insert(any(NotificationPO.class)))
                    .thenThrow(new RuntimeException("DB down"));

            NotificationEvent event = NotificationEvent.builder()
                    .receiverId(100L)
                    .senderId(200L)
                    .type("COMMENT_REPLIED")
                    .createTime(System.currentTimeMillis())
                    .build();

            consumer.onEvent(event, channel, tag);

            verify(channel).basicReject(eq(tag), eq(false));
            verify(channel, never()).basicAck(eq(tag), eq(false));
        }
    }
}
