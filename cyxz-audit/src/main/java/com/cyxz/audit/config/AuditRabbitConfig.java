package com.cyxz.audit.config;

import com.cyxz.audit.api.constant.AuditConstants;
import com.cyxz.common.config.AbstractDlxRabbitConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 审计中心 RabbitMQ 配置（消费端）
 * <p>声明审计日志主队列并绑定到审计 Topic Exchange，通过 {@value AuditConstants#ROUTING_KEY} 接收审计事件。
 * <p>主队列声明死信转发，处理失败的消息自动入死信队列，避免审计事件丢失。
 * <p>所有 Bean 名称加 "audit" 前缀，避免与 AnalyticsRabbitConfig 冲突。
 */
@Configuration
public class AuditRabbitConfig extends AbstractDlxRabbitConfig {

    @Override
    protected String exchangeName() {
        return AuditConstants.EXCHANGE;
    }

    @Override
    protected String queueName() {
        return AuditConstants.QUEUE;
    }

    @Override
    protected String routingKey() {
        return AuditConstants.ROUTING_KEY;
    }

    @Override
    protected String dlxName() {
        return AuditConstants.DLX;
    }

    @Override
    protected String dlqName() {
        return AuditConstants.DLQ;
    }

    @Override
    protected String deadRoutingKey() {
        return AuditConstants.DEAD_ROUTING_KEY;
    }

    @Override
    @Bean(name = "auditMainExchange")
    public TopicExchange mainExchange() {
        return super.mainExchange();
    }

    @Override
    @Bean(name = "auditMainQueue")
    public Queue mainQueue() {
        return super.mainQueue();
    }

    @Override
    @Bean(name = "auditMainBinding")
    public Binding mainBinding() {
        return super.mainBinding();
    }

    @Override
    @Bean(name = "auditDlxExchange")
    public TopicExchange dlxExchange() {
        return super.dlxExchange();
    }

    @Override
    @Bean(name = "auditDlqQueue")
    public Queue dlqQueue() {
        return super.dlqQueue();
    }

    @Override
    @Bean(name = "auditDlqBinding")
    public Binding dlqBinding() {
        return super.dlqBinding();
    }
}
