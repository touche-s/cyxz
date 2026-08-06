package com.cyxz.auth.config;

import com.cyxz.common.security.HeaderAuthenticationFilter;
import com.cyxz.common.security.SecurityUtils;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
 * <p>{@link HeaderAuthenticationFilter} 读网关信任头组装 SecurityContext，使 {@code @PreAuthorize} 生效；
 * {@link AdminRoleFilter} 对 /auth/admin/** 校验 X-User-Role 头，与 Controller 上的
 * {@code @PreAuthorize} 注解形成双重防护，避免漏加注解导致越权。
 */
@Configuration
@EnableMethodSecurity
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
                .addFilterBefore(new HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new AdminRoleFilter(), HeaderAuthenticationFilter.class)
                .anonymous(Customizer.withDefaults());
        return http.build();
    }

    /**
     * 管理员角色校验过滤器
     * <p>对 /auth/admin/** 路径校验 Gateway 注入的 X-User-Roles 头是否包含全局管理员角色
     * （SITE_OWNER / PLATFORM_ADMIN），非管理员直接返回 403。
     * 与 Controller 上的 @PreAuthorize 注解形成纵深防护。
     */
    static class AdminRoleFilter extends OncePerRequestFilter {
        private static final String ADMIN_PATH_PREFIX = "/auth/admin/";

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            String path = request.getRequestURI();
            if (path != null && path.startsWith(ADMIN_PATH_PREFIX)) {
                String rolesHeader = request.getHeader("X-User-Roles");
                boolean isAdmin = false;
                if (rolesHeader != null) {
                    isAdmin = Arrays.stream(rolesHeader.split(","))
                            .anyMatch(SecurityUtils.GLOBAL_ADMIN_ROLES::contains);
                }
                if (!isAdmin) {
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

