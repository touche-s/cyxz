package com.cyxz.auth.config;

import com.cyxz.auth.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 * <p>从配置文件读取 JWT 密钥和过期时间，并注入到 JwtUtil 中
 */
@Configuration
@Data
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400}")
    private long expirationSeconds;

    private final JwtUtil jwtUtil;

    public JwtConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void init() {
        jwtUtil.init(secret, expirationSeconds);
    }
}
