package com.cyxz.post.consumer;

import com.cyxz.common.consumer.AbstractManualAckRabbitListener;
import com.cyxz.governance.api.constant.GovernanceConstants;
import com.cyxz.governance.api.event.ContentTakedownEvent;
import com.cyxz.message.enums.NotificationTargetType;
import com.cyxz.message.enums.NotificationType;
import com.cyxz.message.utils.NotificationPublisher;
import com.cyxz.post.constant.PostTakedownConstants;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.impl.PostCommandService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 帖子处置事件消费者
 * <p>消费治理中心发布的 {@link ContentTakedownEvent}，对目标类型为 POST 的事件
 * 执行软删除，并向帖子作者与举报人发送通知。
 * <p>非 POST 类型事件（COMMENT）静默跳过，由评论服务消费。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostTakedownConsumer extends AbstractManualAckRabbitListener<ContentTakedownEvent> {

    private final PostCommandService postCommandService;
    private final PostMapper postMapper;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = PostTakedownConstants.QUEUE, ackMode = "MANUAL")
    public void onEvent(ContentTakedownEvent event, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        processWithManualAck(event, channel, tag);
    }

    /**
     * 死信监听：记录日志与报警，不再重试
     */
    @RabbitListener(queues = PostTakedownConstants.DLQ)
    public void onDeadLetter(ContentTakedownEvent event) {
        log.error("帖子处置死信: targetId={}, reportId={}, targetType={}",
                event.getTargetId(), event.getReportId(), event.getTargetType());
    }

    @Override
    protected void handle(ContentTakedownEvent event) {
        if (!GovernanceConstants.TARGET_POST.equals(event.getTargetType())) {
            return; // COMMENT 类型由评论服务处理
        }

        // 先查帖子信息（用于通知作者），再执行软删除
        PostPO post = postMapper.selectById(event.getTargetId());
        if (post == null) {
            log.warn("帖子处置: 帖子不存在或已删除 targetId={}", event.getTargetId());
            return;
        }

        postCommandService.adminDeletePost(event.getTargetId());

        // 通知帖子作者
        NotificationPublisher.publish(rabbitTemplate, NotificationPublisher.of(
                post.getUserId(),
                event.getOperatorId(),
                NotificationType.POST_TAKEDOWN,
                "你的帖子因违规被下架",
                NotificationTargetType.POST,
                event.getTargetId()
        ));

        // 通知举报人处理结果
        if (event.getReporterId() != null && !event.getReporterId().equals(post.getUserId())) {
            NotificationPublisher.publish(rabbitTemplate, NotificationPublisher.of(
                    event.getReporterId(),
                    event.getOperatorId(),
                    NotificationType.REPORT_RESULT,
                    "你的举报已处理，相关帖子已被下架",
                    NotificationTargetType.POST,
                    event.getTargetId()
            ));
        }

        log.info("帖子处置完成: postId={}, reportId={}, operatorId={}",
                event.getTargetId(), event.getReportId(), event.getOperatorId());
    }

    @Override
    protected String describe(ContentTakedownEvent event) {
        return "ContentTakedownEvent(postId=" + event.getTargetId()
                + ", reportId=" + event.getReportId() + ")";
    }
}
