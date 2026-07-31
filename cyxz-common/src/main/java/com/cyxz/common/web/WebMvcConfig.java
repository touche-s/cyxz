package com.cyxz.common.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 注册 {@link CurrentUserResolver}。
 * <p>仅 Servlet（Spring MVC）环境生效，Gateway 等 Reactive 服务不会加载。
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 注册 {@link CurrentUserResolver} 参数解析器。
     *
     * @param resolvers 参数解析器注册列表
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserResolver());
        resolvers.add(new AdminUserResolver());
    }
}
