package com.cyxz.audit.config;

import com.cyxz.common.security.AuthPermissionPort;
import com.cyxz.common.security.BaseSecurityConfig;
import com.cyxz.common.security.GlobalPermissionProvider;
import com.cyxz.common.security.GlobalPermissionProviderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 审计中心安全配置
 * <p>继承 {@link BaseSecurityConfig} 复用无状态 + 全放行 + 信任头过滤器的通用配置，
 * 授权完全交给 {@code @PreAuthorize} 方法注解（全局权限码）。
 * <p>审计中心为平台级服务，不涉及圈子权限，故无需 {@code CirclePermissionEvaluator}。
 * <p>全局权限通过 {@link GlobalPermissionProvider} 从 Redis/DB 加载（Cache-Aside），
 * 未命中时经 {@link AuthPermissionPort}（Feign → auth 服务）查询。
 * <p>缓存 TTL 由 {@link com.cyxz.common.security.TokenTtlContext} 从请求头 {@code X-Token-Remaining}
 * 获取，对齐当前 Token 剩余时间。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig extends BaseSecurityConfig {

    @Bean
    public GlobalPermissionProvider globalPermissionProvider(AuthPermissionPort authPermissionPort,
                                                              StringRedisTemplate redisTemplate) {
        return new GlobalPermissionProviderImpl(authPermissionPort, redisTemplate);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    GlobalPermissionProvider permissionProvider) throws Exception {
        return applyBase(http, permissionProvider).build();
    }
}
