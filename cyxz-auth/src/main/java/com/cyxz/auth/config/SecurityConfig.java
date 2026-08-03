package com.cyxz.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 安全配置
 * <p>认证入口由 Gateway 统一暴露，auth 服务本身仅显式放行认证相关接口，
 * 并关闭默认表单登录、BasicAuth 和 Session。
 * <p>/auth/admin/** 路径由 {@link AdminRoleFilter} 校验 X-User-Role 头，
 * 与 Controller 上的 @AdminUser 注解形成双重防护，避免漏加注解导致越权。
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/auth/login",
                                "/auth/register",
                                "/auth/logout",
                                "/auth/refresh"
                        ).permitAll()
                        .requestMatchers(HttpMethod.PUT,
                                "/auth/password"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/captcha/**").permitAll()
                        .requestMatchers("/auth/admin/**").permitAll()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(new AdminRoleFilter(), UsernamePasswordAuthenticationFilter.class)
                .anonymous(Customizer.withDefaults());
        return http.build();
    }

    /**
     * 管理员角色校验过滤器
     * <p>对 /auth/admin/** 路径校验 Gateway 注入的 X-User-Role 头是否为 admin，
     * 非 admin 直接返回 403。即使 Controller 漏加 @AdminUser 注解也不会越权。
     */
    static class AdminRoleFilter extends OncePerRequestFilter {
        private static final String ADMIN_PATH_PREFIX = "/auth/admin/";

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            String path = request.getRequestURI();
            if (path != null && path.startsWith(ADMIN_PATH_PREFIX)) {
                String role = request.getHeader("X-User-Role");
                if (!"admin".equals(role)) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"code\":403,\"message\":\"仅管理员可执行此操作\"}");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        }
    }

    /**
     * BCrypt 密码编码器
     *
     * @return PasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
