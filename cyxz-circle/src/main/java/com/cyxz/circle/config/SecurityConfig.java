package com.cyxz.circle.config;

import com.cyxz.common.security.BaseSecurityConfig;
import com.cyxz.common.security.CirclePermissionEvaluator;
import com.cyxz.common.security.mapper.PermissionQueryMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 圈子服务安全配置
 * <p>继承 {@link BaseSecurityConfig} 复用无状态 + 全放行 + 信任头过滤器的通用配置，
 * 授权完全交给 {@code @PreAuthorize} 方法注解（全局权限码 + {@code @circlePerm} 圈子权限）。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig extends BaseSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return applyBase(http).build();
    }

    /** 圈子权限校验器，供 @PreAuthorize("@circlePerm.hasAuthority(...)") 调用 */
    @Bean(name = "circlePerm")
    public CirclePermissionEvaluator circlePermissionEvaluator(PermissionQueryMapper mapper) {
        return new CirclePermissionEvaluator(mapper);
    }
}
