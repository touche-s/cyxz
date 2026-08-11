package com.cyxz.comment.consumer;

import com.cyxz.comment.constant.CommentTakedownConstants;
import com.cyxz.comment.entity.CommentPO;
import com.cyxz.comment.mapper.CommentMapper;
import com.cyxz.comment.service.CommentService;
import com.cyxz.common.consumer.AbstractManualAckRabbitListener;
import com.cyxz.governance.api.constant.GovernanceConstants;
import com.cyxz.governance.api.event.ContentTakedownEvent;
import com.cyxz.message.enums.NotificationTargetType;
import com.cyxz.message.enums.NotificationType;
import com.cyxz.message.utils.NotificationPublisher;
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
 * 评论处置事件消费者
 * <p>消费治理中心发布的 {@link ContentTakedownEvent}，对目标类型为 COMMENT 的事件
 * 执行逻辑删除，并向评论作者与举报人发送通知。
 * <p>非 COMMENT 类型事件（POST）静默跳过，由帖子服务消费。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentTakedownConsumer extends AbstractManualAckRabbitListener<ContentTakedownEvent> {

    private final CommentService commentService;
    private final CommentMapper commentMapper;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = CommentTakedownConstants.QUEUE, ackMode = "MANUAL")
    public void onEvent(ContentTakedownEvent event, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        processWithManualAck(event, channel, tag);
    }

    /**
     * 死信监听：记录日志，不再重试
     */
    @RabbitListener(queues = CommentTakedownConstants.DLQ)
    public void onDeadLetter(ContentTakedownEvent event) {
        log.error("评论处置死信: targetId={}, reportId={}, targetType={}",
                event.getTargetId(), event.getReportId(), event.getTargetType());
    }

    @Override
    protected void handle(ContentTakedownEvent event) {
        if (!GovernanceConstants.TARGET_COMMENT.equals(event.getTargetType())) {
            return; // POST 类型由帖子服务处理
        }

        // 先查评论信息（用于通知作者），再执行删除
        CommentPO comment = commentMapper.selectById(event.getTargetId());
        if (comment == null) {
            log.warn("评论处置: 评论不存在或已删除 targetId={}", event.getTargetId());
            return;
        }

        commentService.adminDeleteComment(event.getTargetId());

        // 通知评论作者
        NotificationPublisher.publish(rabbitTemplate, NotificationPublisher.of(
                comment.getUserId(),
                event.getOperatorId(),
                NotificationType.COMMENT_TAKEDOWN,
                "你的评论因违规被删除",
                NotificationTargetType.COMMENT,
                event.getTargetId()
        ));

        // 通知举报人处理结果（不与作者重复通知）
        if (event.getReporterId() != null && !event.getReporterId().equals(comment.getUserId())) {
            NotificationPublisher.publish(rabbitTemplate, NotificationPublisher.of(
                    event.getReporterId(),
                    event.getOperatorId(),
                    NotificationType.REPORT_RESULT,
                    "你的举报已处理，相关评论已被删除",
                    NotificationTargetType.COMMENT,
                    event.getTargetId()
            ));
        }

        log.info("评论处置完成: commentId={}, reportId={}, operatorId={}",
                event.getTargetId(), event.getReportId(), event.getOperatorId());
    }

    @Override
    protected String describe(ContentTakedownEvent event) {
        return "ContentTakedownEvent(commentId=" + event.getTargetId()
                + ", reportId=" + event.getReportId() + ")";
    }
}
