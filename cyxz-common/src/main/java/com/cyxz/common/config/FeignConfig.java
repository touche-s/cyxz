package com.cyxz.common.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器
 * <p>在 Feign 发起跨服务调用时，透传 X-User-Id 与 X-Token-Remaining 头到下游服务，
 * 实现调用链上的用户身份与 Token 剩余时间传递（下游据此为权限缓存设置 TTL）。
 */
@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignConfig implements RequestInterceptor {

    /**
     * 拦截 Feign 请求，注入 X-User-Id 请求头
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
            String tokenRemaining = attributes.getRequest().getHeader("X-Token-Remaining");
            if (tokenRemaining != null) {
                template.header("X-Token-Remaining", tokenRemaining);
            }
        }
    }
}
