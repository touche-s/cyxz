package com.cyxz.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;

/**
 * 死信队列（DLX）RabbitMQ 配置抽象基类
 * <p>统一声明"主交换机 + 主队列 + 主绑定 + 死信交换机 + 死信队列 + 死信绑定"六件套，
 * 子类只需提供各资源的名称即可获得完整 DLX 配置。
 */
public abstract class AbstractDlxRabbitConfig {

    protected abstract String exchangeName();

    protected abstract String queueName();

    protected abstract String routingKey();

    protected abstract String dlxName();

    protected abstract String dlqName();

    protected abstract String deadRoutingKey();

    @Bean
    public TopicExchange mainExchange() {
        return new TopicExchange(exchangeName(), true, false);
    }

    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(queueName())
                .deadLetterExchange(dlxName())
                .deadLetterRoutingKey(deadRoutingKey())
                .build();
    }

    @Bean
    public Binding mainBinding() {
        return BindingBuilder.bind(mainQueue()).to(mainExchange()).with(routingKey());
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(dlxName(), true, false);
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(dlqName()).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with(deadRoutingKey());
    }
}
