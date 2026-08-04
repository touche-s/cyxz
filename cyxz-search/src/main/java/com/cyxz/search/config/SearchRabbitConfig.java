package com.cyxz.search.config;

import com.cyxz.common.config.AbstractDlxRabbitConfig;
import com.cyxz.common.constant.EsSyncConstants;
import org.springframework.context.annotation.Configuration;

/**
 * 搜索服务 RabbitMQ 配置
 * <p>继承 {@link AbstractDlxRabbitConfig} 获得 ES 同步主队列与死信队列的完整声明。
 */
@Configuration
public class SearchRabbitConfig extends AbstractDlxRabbitConfig {

    @Override
    protected String exchangeName() {
        return EsSyncConstants.EXCHANGE;
    }

    @Override
    protected String queueName() {
        return EsSyncConstants.QUEUE;
    }

    @Override
    protected String routingKey() {
        return EsSyncConstants.ROUTING_KEY;
    }

    @Override
    protected String dlxName() {
        return EsSyncConstants.DLX;
    }

    @Override
    protected String dlqName() {
        return EsSyncConstants.DLQ;
    }

    @Override
    protected String deadRoutingKey() {
        return EsSyncConstants.DEAD_ROUTING_KEY;
    }
}
