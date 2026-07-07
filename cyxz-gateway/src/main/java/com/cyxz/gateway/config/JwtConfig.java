package com.cyxz.gateway.config;

import com.cyxz.gateway.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 * <p>从配置文件读取 JWT 密钥和过期时间，并注入到 JwtUtil 中
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    private final JwtUtil jwtUtil;

    public JwtConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void init() {
        jwtUtil.init(secret);
    }
}
