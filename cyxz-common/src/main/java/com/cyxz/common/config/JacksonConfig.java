package com.cyxz.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Jackson 序列化配置
 * <p>Long 类型字段转为字符串输出，防止前端 JavaScript 精度丢失；
 * 时区固定为 GMT+8。
 */
@Configuration
public class JacksonConfig {

    /**
     * Jackson 自定义配置
     *
     * @return Jackson2ObjectMapperBuilderCustomizer 实例
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            builder.timeZone(TimeZone.getTimeZone("GMT+8"));
        };
    }
}
