package com.cyxz.auth.config;

import com.cyxz.auth.mapper.SysUserRoleMapper;
import com.cyxz.auth.service.AuthPermissionPortImpl;
import com.cyxz.common.security.AuthPermissionPort;
import com.cyxz.common.security.GlobalPermissionProvider;
import com.cyxz.common.security.GlobalPermissionProviderImpl;
import com.cyxz.common.security.HeaderAuthenticationFilter;
import com.cyxz.common.security.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
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

import java.io.IOException;

/**
 * 安全配置
 * <p>认证入口由 Gateway 统一暴露，auth 服务本身仅显式放行认证相关接口，
 * 并关闭默认表单登录、BasicAuth 和 Session。
 * <p>{@link HeaderAuthenticationFilter} 读网关信任头，通过 {@link GlobalPermissionProvider}
 * 从 Redis/DB 加载权限后组装 SecurityContext，使 {@code @PreAuthorize} 生效。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public GlobalPermissionProvider globalPermissionProvider(AuthPermissionPort authPermissionPort,
                                                              StringRedisTemplate redisTemplate) {
        return new GlobalPermissionProviderImpl(authPermissionPort, redisTemplate);
    }

    @Bean
    public AuthPermissionPort authPermissionPort(SysUserRoleMapper sysUserRoleMapper) {
        return new AuthPermissionPortImpl(sysUserRoleMapper);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    GlobalPermissionProvider permissionProvider) throws Exception {
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
                        .requestMatchers("/auth/internal/**").permitAll()
                        .requestMatchers("/auth/admin/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/doc.html",
                                "/webjars/**",
                                "/favicon.ico",
                                "/swagger-resources/**"
                        ).permitAll()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(new HeaderAuthenticationFilter(permissionProvider), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new AdminRoleFilter(), HeaderAuthenticationFilter.class)
                .anonymous(Customizer.withDefaults());
        return http.build();
    }

    /**
     * 管理员角色校验过滤器
     * <p>对 /auth/admin/** 路径校验 SecurityContext 中的角色是否为全局管理员
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
                if (!SecurityUtils.isGlobalAdmin()) {
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
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
