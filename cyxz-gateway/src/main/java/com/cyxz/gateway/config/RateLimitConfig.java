package com.cyxz.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 网关限流配置
 * <p>基于 Redis 令牌桶（RequestRateLimiter），按客户端 IP 维度限流，
 * 超载时网关直接拒绝请求，避免请求穿透到后端拖垮服务。
 */
@Configuration
public class RateLimitConfig {

    /** 每秒补充令牌数（QPS 上限） */
    @Value("${app.rate-limit.replenish-rate:500}")
    private int replenishRate;

    /** 突发容量（允许的瞬时峰值） */
    @Value("${app.rate-limit.burst-capacity:600}")
    private int burstCapacity;

    /**
     * 按客户端 IP 限流
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress().getHostAddress());
    }

    /**
     * 默认令牌桶限流器
     */
    @Bean
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }
}
