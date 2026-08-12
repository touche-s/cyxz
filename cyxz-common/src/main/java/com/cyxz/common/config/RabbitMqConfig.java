package com.cyxz.common.config;

import com.cyxz.common.trace.TraceIdContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * RabbitMQ 全局配置
 * <p>为 RabbitTemplate 注册 MessagePostProcessor，在每条发送的消息头上自动注入当前线程的 traceId，
 * 消费者可从消息头提取 traceId 实现跨 MQ 的链路追踪。
 */
@Slf4j
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitMqConfig {

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setBeforePublishPostProcessors(new TraceIdMessagePostProcessor());
        return template;
    }

    /**
     * 消息发送前注入 X-Trace-Id 头
     */
    private static class TraceIdMessagePostProcessor implements MessagePostProcessor {
        @Override
        public Message postProcessMessage(Message message) {
            String traceId = TraceIdContext.get();
            if (traceId != null) {
                message.getMessageProperties().setHeader("X-Trace-Id", traceId);
            }
            return message;
        }
    }
}
