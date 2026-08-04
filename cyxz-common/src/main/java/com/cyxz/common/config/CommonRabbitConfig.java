package com.cyxz.common.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 公共配置
 * <p>统一 Jackson 消息转换器，消除各 MQ 模块重复声明的 Bean。
 * <p>仅在 classpath 存在 amqp 时生效，不影响不使用 RabbitMQ 的模块。
 */
@Configuration
@ConditionalOnClass(MessageConverter.class)
public class CommonRabbitConfig {

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
