package com.cyxz.analytics.config;

import com.cyxz.common.config.AbstractDlxRabbitConfig;
import com.cyxz.common.constant.AnalyticsConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统计服务 RabbitMQ 配置
 * <p>继承 {@link AbstractDlxRabbitConfig} 获得主队列与死信队列的完整声明，
 * 消费各业务服务发布的 {@link com.cyxz.common.event.AnalyticsEvent} 统计事件。
 * <p>所有 Bean 名称加 "analytics" 前缀，避免与 AuditRabbitConfig 冲突。
 */
@Configuration
public class AnalyticsRabbitConfig extends AbstractDlxRabbitConfig {

    @Override
    protected String exchangeName() {
        return AnalyticsConstants.EXCHANGE;
    }

    @Override
    protected String queueName() {
        return AnalyticsConstants.QUEUE;
    }

    @Override
    protected String routingKey() {
        return AnalyticsConstants.ROUTING_KEY;
    }

    @Override
    protected String dlxName() {
        return AnalyticsConstants.DLX;
    }

    @Override
    protected String dlqName() {
        return AnalyticsConstants.DLQ;
    }

    @Override
    protected String deadRoutingKey() {
        return AnalyticsConstants.DEAD_ROUTING_KEY;
    }

    @Override
    @Bean(name = "analyticsMainExchange")
    public TopicExchange mainExchange() {
        return super.mainExchange();
    }

    @Override
    @Bean(name = "analyticsMainQueue")
    public Queue mainQueue() {
        return super.mainQueue();
    }

    @Override
    @Bean(name = "analyticsMainBinding")
    public Binding mainBinding() {
        return super.mainBinding();
    }

    @Override
    @Bean(name = "analyticsDlxExchange")
    public TopicExchange dlxExchange() {
        return super.dlxExchange();
    }

    @Override
    @Bean(name = "analyticsDlqQueue")
    public Queue dlqQueue() {
        return super.dlqQueue();
    }

    @Override
    @Bean(name = "analyticsDlqBinding")
    public Binding dlqBinding() {
        return super.dlqBinding();
    }
}
