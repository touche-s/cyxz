package com.cyxz.message.config;

import com.cyxz.auth.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 * <p>从配置文件读取 JWT 密钥并注入到 JwtUtil，供 WebSocket 握手鉴权使用。
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
