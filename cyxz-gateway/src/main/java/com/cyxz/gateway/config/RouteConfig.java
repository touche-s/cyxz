package com.cyxz.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由配置
 * <p>路由全部通过 Java Config 定义，避免 Nacos 远程配置覆盖本地路由。
 * <p>所有 /internal/ 路径不对外暴露，仅供服务间 Feign 内部调用。
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                           RedisRateLimiter redisRateLimiter,
                                           KeyResolver ipKeyResolver) {
        return builder.routes()
                .route("auth", r -> r.path("/api/auth/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-auth"))
                .route("user", r -> r.path("/api/user/**", "/api/history/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-user"))
                .route("post-admin", r -> r.path("/api/admin/post/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-post"))
                .route("post", r -> r.path("/api/post/**", "/api/posts/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter)
                                        .setKeyResolver(ipKeyResolver)))
                        .uri("lb://cyxz-post"))
                .route("circle", r -> r.path("/api/circle/**", "/api/admin/circle/**", "/api/section/**", "/api/admin/section-template/**",
                                "/api/circle-application/**", "/api/admin/circle-application/**",
                                "/api/circle-join-application/**", "/api/admin/circle-join-application/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-circle"))
                .route("comment", r -> r.path("/api/comment/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-comment"))
                .route("message", r -> r.path("/api/message/**", "/api/notification/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-message"))
                .route("search", r -> r.path("/api/search/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-search"))
                .route("upload", r -> r.path("/api/upload/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-upload"))
                .route("governance-admin", r -> r.path("/api/admin/report/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-governance"))
                .route("governance", r -> r.path("/api/report/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-governance"))
                .route("audit", r -> r.path("/api/admin/audit/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-audit"))
                .route("analytics", r -> r.path("/api/admin/analytics/**")
                        .and().not(p -> p.path("/api/*/internal/**"))
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cyxz-analytics"))
                .route("chat-ws", r -> r.path("/ws/message/**")
                        .uri("lb://cyxz-message"))
                .build();
    }
}
