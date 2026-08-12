package com.cyxz.search.consumer;

import com.cyxz.common.consumer.AbstractManualAckRabbitListener;
import com.cyxz.common.constant.EsSyncConstants;
import com.cyxz.common.event.PostEsSyncEvent;
import com.cyxz.search.service.PostIndexService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 消费帖子 ES 同步事件，将帖子数据同步到 Elasticsearch
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostEsSyncConsumer extends AbstractManualAckRabbitListener<PostEsSyncEvent> {

    private final PostIndexService postIndexService;

    @RabbitListener(queues = EsSyncConstants.QUEUE, ackMode = "MANUAL")
    public void onSyncEvent(PostEsSyncEvent event, Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long tag, Message message) throws IOException {
        processWithManualAck(event, channel, tag, message);
    }

    @Override
    protected void handle(PostEsSyncEvent event) throws Exception {
        postIndexService.sync(event);
    }

    @Override
    protected String describe(PostEsSyncEvent event) {
        return "postId=" + event.getPostId() + ", action=" + event.getAction();
    }

    /**
     * 死信队列消费者：ES 同步失败的消息进入死信，记录日志便于人工排查或重放
     */
    @RabbitListener(queues = EsSyncConstants.DLQ)
    public void onDeadLetter(PostEsSyncEvent event) {
        log.error("ES 同步消息进入死信，需人工处理: postId={}, action={}", event.getPostId(), event.getAction());
    }
}
