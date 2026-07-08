package com.cyxz.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由配置
 * <p>路由全部通过 Java Config 定义，避免 Nacos 远程配置覆盖本地路由。
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth", r -> r.path("/api/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-auth"))
                .route("user", r -> r.path("/api/user/**", "/api/history/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-user"))
                .route("post", r -> r.path("/api/posts/**", "/api/comment/**", "/api/category/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-post"))
                .route("message", r -> r.path("/api/message/**", "/api/notification/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-message"))
                .route("search", r -> r.path("/api/search/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-search"))
                .route("upload", r -> r.path("/api/upload/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-upload"))
                .build();
    }
}
