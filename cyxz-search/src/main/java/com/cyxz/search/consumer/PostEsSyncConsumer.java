package com.cyxz.search.consumer;

import com.cyxz.common.constant.EsSyncConstants;
import com.cyxz.common.event.PostEsSyncEvent;
import com.cyxz.search.service.PostIndexService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class PostEsSyncConsumer {

    private final PostIndexService postIndexService;

    @RabbitListener(queues = EsSyncConstants.QUEUE, ackMode = "MANUAL")
    public void onSyncEvent(PostEsSyncEvent event, Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            postIndexService.sync(event);
        } catch (Exception e) {
            log.error("ES 同步失败: postId={}, action={}", event.getPostId(), event.getAction(), e);
            // 重试一次失败后拒绝，不进死信
            channel.basicReject(tag, false);
            return;
        }
        channel.basicAck(tag, false);
    }
}
