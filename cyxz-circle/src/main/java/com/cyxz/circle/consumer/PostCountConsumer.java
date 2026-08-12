package com.cyxz.circle.consumer;

import com.cyxz.circle.mapper.CircleMapper;
import com.cyxz.common.constant.PostCountConstants;
import com.cyxz.common.consumer.AbstractManualAckRabbitListener;
import com.cyxz.common.event.PostCountEvent;
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

/**
 * 消费帖子计数变更事件，增量更新圈子 post_count
 * <p>替代原定时 Feign 全量拉取，破除 circle→post 的循环依赖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostCountConsumer extends AbstractManualAckRabbitListener<PostCountEvent> {

    private final CircleMapper circleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DEDUP_PREFIX = "postcount:dedup:";
    private static final Duration DEDUP_TTL = Duration.ofHours(24);

    @RabbitListener(queues = PostCountConstants.QUEUE, ackMode = "MANUAL")
    public void onPostCountEvent(PostCountEvent event, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        processWithManualAck(event, channel, tag);
    }

    @Override
    protected void handle(PostCountEvent event) throws Exception {
        // event_id 防重：SETNX 失败则跳过
        if (event.getEventId() != null) {
            Boolean first = stringRedisTemplate.opsForValue()
                    .setIfAbsent(DEDUP_PREFIX + event.getEventId(), "1", DEDUP_TTL);
            if (Boolean.FALSE.equals(first)) {
                log.debug("计数事件重复跳过: eventId={}, postId={}", event.getEventId(), event.getPostId());
                return;
            }
        }
        int delta = PostCountConstants.ACTION_PUBLISH.equals(event.getAction()) ? 1 : -1;
        circleMapper.updatePostCount(event.getCircleId(), delta);
        log.debug("圈子帖子计数更新: circleId={}, delta={}, postId={}", event.getCircleId(), delta, event.getPostId());
    }

    @Override
    protected String describe(PostCountEvent event) {
        return "action=" + event.getAction() + ", circleId=" + event.getCircleId() + ", postId=" + event.getPostId();
    }

    @RabbitListener(queues = PostCountConstants.DLQ)
    public void onDeadLetter(PostCountEvent event) {
        log.error("帖子计数事件进入死信，需人工处理: action={}, circleId={}, postId={}",
                event.getAction(), event.getCircleId(), event.getPostId());
    }
}
