package com.cyxz.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置
 * <p>认证入口由 Gateway 统一暴露，auth 服务本身仅显式放行认证相关接口，
 * 并关闭默认表单登录、BasicAuth 和 Session。
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
                .anonymous(Customizer.withDefaults());
        return http.build();
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
