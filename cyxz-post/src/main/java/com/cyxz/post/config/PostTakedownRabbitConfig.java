package com.cyxz.post.config;

import com.cyxz.common.config.AbstractDlxRabbitConfig;
import com.cyxz.governance.api.constant.GovernanceConstants;
import com.cyxz.post.constant.PostTakedownConstants;
import org.springframework.context.annotation.Configuration;

/**
 * 帖子处置 RabbitMQ 配置（消费端）
 * <p>声明帖子处置队列并绑定到治理中心的 Topic Exchange，
 * 通过 {@value GovernanceConstants#ROUTING_KEY} 接收内容处置事件。
 * <p>主队列声明死信转发，处理失败的消息自动入死信队列。
 */
@Configuration
public class PostTakedownRabbitConfig extends AbstractDlxRabbitConfig {

    @Override
    protected String exchangeName() {
        return GovernanceConstants.EXCHANGE;
    }

    @Override
    protected String queueName() {
        return PostTakedownConstants.QUEUE;
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
        return PostTakedownConstants.DLQ;
    }

    @Override
    protected String deadRoutingKey() {
        return PostTakedownConstants.DEAD_ROUTING_KEY;
    }
}
