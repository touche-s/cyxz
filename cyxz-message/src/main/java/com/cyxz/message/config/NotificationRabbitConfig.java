package com.cyxz.message.config;

import com.cyxz.common.config.AbstractDlxRabbitConfig;
import com.cyxz.message.constant.NotificationConstants;
import org.springframework.context.annotation.Configuration;

/**
 * 通知服务 RabbitMQ 配置
 * <p>继承 {@link AbstractDlxRabbitConfig} 获得主队列与死信队列的完整声明。
 */
@Configuration
public class NotificationRabbitConfig extends AbstractDlxRabbitConfig {

    @Override
    protected String exchangeName() {
        return NotificationConstants.EXCHANGE;
    }

    @Override
    protected String queueName() {
        return NotificationConstants.QUEUE;
    }

    @Override
    protected String routingKey() {
        return NotificationConstants.ROUTING_KEY;
    }

    @Override
    protected String dlxName() {
        return NotificationConstants.DLX;
    }

    @Override
    protected String dlqName() {
        return NotificationConstants.DLQ;
    }

    @Override
    protected String deadRoutingKey() {
        return NotificationConstants.DEAD_ROUTING_KEY;
    }
}
