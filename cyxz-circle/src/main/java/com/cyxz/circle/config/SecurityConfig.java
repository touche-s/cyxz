package com.cyxz.circle.config;

import com.cyxz.circle.feign.CircleExistsPort;
import com.cyxz.common.security.AuthPermissionPort;
import com.cyxz.common.security.BaseSecurityConfig;
import com.cyxz.common.security.CirclePermissionEvaluator;
import com.cyxz.common.security.GlobalPermissionProvider;
import com.cyxz.common.security.GlobalPermissionProviderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 圈子服务安全配置
 * <p>继承 {@link BaseSecurityConfig} 复用无状态 + 全放行 + 信任头过滤器的通用配置，
 * 授权完全交给 {@code @PreAuthorize} 方法注解（全局权限码 + {@code @circlePerm} 圈子权限）。
 * <p>全局权限经 {@link GlobalPermissionProvider} + {@link AuthPermissionPort}（Feign 调 auth）加载（Cache-Aside），
 * 圈子权限由 {@code @circlePerm} 经 auth（角色/权限）与 circle（存在性）校验。
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

    /** 圈子权限校验器，供 @PreAuthorize("@circlePerm.hasAuthority(...)") 调用 */
    @Bean(name = "circlePerm")
    public CirclePermissionEvaluator circlePermissionEvaluator(AuthPermissionPort authPermissionPort,
                                                                CircleExistsPort circleExistsPort,
                                                                StringRedisTemplate redisTemplate) {
        return new CirclePermissionEvaluator(authPermissionPort, circleExistsPort, redisTemplate);
    }
}
