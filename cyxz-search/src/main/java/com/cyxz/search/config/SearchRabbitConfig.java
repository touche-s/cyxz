package com.cyxz.search.config;

import com.cyxz.common.constant.EsSyncConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
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
        return new Queue(EsSyncConstants.QUEUE, true);
    }

    @Bean
    public Binding postEsSyncBinding() {
        return BindingBuilder.bind(postEsSyncQueue()).to(postExchange()).with(EsSyncConstants.ROUTING_KEY);
    }

    @Bean
    public MessageConverter postEsMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
