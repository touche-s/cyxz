package com.cyxz.audit.config;

import com.cyxz.audit.api.constant.AuditConstants;
import com.cyxz.common.config.AbstractDlxRabbitConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 审计中心 RabbitMQ 配置（消费端）
 * <p>声明审计日志主队列并绑定到审计 Topic Exchange，通过 {@value AuditConstants#ROUTING_KEY} 接收审计事件。
 * <p>主队列声明死信转发，处理失败的消息自动入死信队列，避免审计事件丢失。
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
}
