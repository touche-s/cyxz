package com.cyxz.circle.config;

import com.cyxz.common.config.AbstractDlxRabbitConfig;
import com.cyxz.common.constant.PostCountConstants;
import org.springframework.context.annotation.Configuration;

/**
 * 圈子服务 RabbitMQ 配置
 * <p>继承 {@link AbstractDlxRabbitConfig} 获得帖子计数事件的主队列与死信队列的完整声明。
 */
@Configuration
public class CircleRabbitConfig extends AbstractDlxRabbitConfig {

    @Override
    protected String exchangeName() {
        return PostCountConstants.EXCHANGE;
    }

    @Override
    protected String queueName() {
        return PostCountConstants.QUEUE;
    }

    @Override
    protected String routingKey() {
        return PostCountConstants.ROUTING_KEY;
    }

    @Override
    protected String dlxName() {
        return PostCountConstants.DLX;
    }

    @Override
    protected String dlqName() {
        return PostCountConstants.DLQ;
    }

    @Override
    protected String deadRoutingKey() {
        return PostCountConstants.DEAD_ROUTING_KEY;
    }
}
