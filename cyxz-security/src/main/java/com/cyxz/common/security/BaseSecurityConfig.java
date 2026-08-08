package com.cyxz.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 微服务 SecurityConfig 基类
 * <p>各 Servlet 微服务的 {@code SecurityConfig} 继承本类，调用 {@link #applyBase(HttpSecurity, GlobalPermissionProvider)}
 * 完成无状态 + 全放行 + 信任头过滤器的通用配置，授权交给 {@code @PreAuthorize} 方法注解。
 * <p>设计为抽象类避免自身被扫描注册造成 SecurityFilterChain 重复 Bean；
 * 子类需标注 {@code @Configuration} 与 {@code @EnableMethodSecurity}。
 */
public abstract class BaseSecurityConfig {

    /**
     * 应用通用安全配置：关闭 CSRF/表单/Basic/登出、无状态会话、全放行、注册信任头过滤器。
     * <p>请求鉴权由 Gateway（认证）+ {@code @PreAuthorize}（授权）负责，
     * 故此处 anyRequest permitAll，不在此处做路径级拦截。
     *
     * @param http              HttpSecurity 构建器
     * @param permissionProvider 全局权限提供者（Redis Cache-Aside + DB 兜底）
     * @return HttpSecurity 构建器（链式）
     * @throws Exception 配置异常
     */
    protected HttpSecurity applyBase(HttpSecurity http, GlobalPermissionProvider permissionProvider) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .addFilterBefore(new HeaderAuthenticationFilter(permissionProvider), UsernamePasswordAuthenticationFilter.class);
    }
}
