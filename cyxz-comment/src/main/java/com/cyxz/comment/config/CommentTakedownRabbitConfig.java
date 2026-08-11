package com.cyxz.comment.config;

import com.cyxz.comment.constant.CommentTakedownConstants;
import com.cyxz.common.config.AbstractDlxRabbitConfig;
import com.cyxz.governance.api.constant.GovernanceConstants;
import org.springframework.context.annotation.Configuration;

/**
 * 评论处置 RabbitMQ 配置（消费端）
 * <p>声明评论处置队列并绑定到治理中心的 Topic Exchange，
 * 通过 {@value GovernanceConstants#ROUTING_KEY} 接收内容处置事件。
 * <p>主队列声明死信转发，处理失败的消息自动入死信队列。
 */
@Configuration
public class CommentTakedownRabbitConfig extends AbstractDlxRabbitConfig {

    @Override
    protected String exchangeName() {
        return GovernanceConstants.EXCHANGE;
    }

    @Override
    protected String queueName() {
        return CommentTakedownConstants.QUEUE;
    }

    @Override
    protected String routingKey() {
        return GovernanceConstants.ROUTING_KEY;
    }

    @Override
    protected String dlxName() {
        return GovernanceConstants.DLX;
    }

    @Override
    protected String dlqName() {
        return CommentTakedownConstants.DLQ;
    }

    @Override
    protected String deadRoutingKey() {
        return CommentTakedownConstants.DEAD_ROUTING_KEY;
    }
}
