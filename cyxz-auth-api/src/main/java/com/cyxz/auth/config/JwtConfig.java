package com.cyxz.auth.config;

import com.cyxz.auth.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 * <p>统一从配置文件读取 JWT 密钥与过期时间并注入 JwtUtil。
 * <p>未配置 jwt.expiration 时仅注入 secret（用于 gateway/message 验签场景），
 * 配置了过期时间则同时注入（用于 auth 签发 Token）。
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:0}")
    private long expirationSeconds;

    private final JwtUtil jwtUtil;

    public JwtConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void init() {
        if (expirationSeconds > 0) {
            jwtUtil.init(secret, expirationSeconds);
        } else {
            jwtUtil.init(secret);
        }
    }
}
