package com.cyxz.common.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String userId = attributes.getRequest().getHeader("X-User-Id");
            if (userId != null) {
                template.header("X-User-Id", userId);
            }
        }
    }
}
