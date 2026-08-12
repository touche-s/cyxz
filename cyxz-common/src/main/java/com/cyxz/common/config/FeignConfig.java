package com.cyxz.common.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Feign 请求拦截器
 * <p>在 Feign 发起跨服务调用时，透传 X-User-Id 与 X-Trace-Id 头到下游服务，
 * 实现调用链上的用户身份传递与全链路追踪。
 */
@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignConfig implements RequestInterceptor {

    /**
     * 拦截 Feign 请求，注入 X-User-Id 和 X-Trace-Id 请求头
     *
     * @param template Feign 请求模板
     */
    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String userId = attributes.getRequest().getHeader("X-User-Id");
            if (userId != null) {
                template.header("X-User-Id", userId);
            }
            String traceId = attributes.getRequest().getHeader("X-Trace-Id");
            if (traceId != null) {
                template.header("X-Trace-Id", traceId);
            }
        } else {
            // 无请求上下文（如定时任务），生成新 traceId
            template.header("X-Trace-Id", UUID.randomUUID().toString().replace("-", ""));
        }
    }
}
