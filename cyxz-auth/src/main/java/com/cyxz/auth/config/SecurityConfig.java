package com.cyxz.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全配置
 * <p>仅提供 BCryptPasswordEncoder Bean，不启用 Spring Security 过滤链。
 * 认证和鉴权由 Gateway 统一处理。
 */
@Configuration
public class SecurityConfig {

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
