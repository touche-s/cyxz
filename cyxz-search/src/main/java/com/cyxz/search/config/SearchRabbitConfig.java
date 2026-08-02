package com.cyxz.search.config;

import com.cyxz.common.constant.EsSyncConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchRabbitConfig {

    @Bean
    public TopicExchange postExchange() {
        return new TopicExchange(EsSyncConstants.EXCHANGE, true, false);
    }

    @Bean
    public Queue postEsSyncQueue() {
        return QueueBuilder.durable(EsSyncConstants.QUEUE)
                .deadLetterExchange(EsSyncConstants.DLX)
                .deadLetterRoutingKey(EsSyncConstants.DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding postEsSyncBinding() {
        return BindingBuilder.bind(postEsSyncQueue()).to(postExchange()).with(EsSyncConstants.ROUTING_KEY);
    }

    /** 死信交换机：ES 同步失败的消息路由到死信队列 */
    @Bean
    public TopicExchange postEsDlx() {
        return new TopicExchange(EsSyncConstants.DLX, true, false);
    }

    @Bean
    public Queue postEsDlq() {
        return QueueBuilder.durable(EsSyncConstants.DLQ).build();
    }

    @Bean
    public Binding postEsDlqBinding() {
        return BindingBuilder.bind(postEsDlq()).to(postEsDlx()).with(EsSyncConstants.DEAD_ROUTING_KEY);
    }

    @Bean
    public MessageConverter postEsMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
