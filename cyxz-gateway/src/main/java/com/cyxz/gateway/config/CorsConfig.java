package com.cyxz.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨域配置（Gateway 专用，使用 Reactive 组件）
 * <p>允许任意来源、任意方法、任意请求头，支持携带认证信息。
 */
@Configuration
public class CorsConfig {

    @Value("#{'${app.security.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}'.split(',')}")
    private List<String> allowedOriginPatterns;

    /**
     * 跨域过滤器
     *
     * @return CorsWebFilter 实例
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 显式列举允许的请求头，不含 X-User-Id/X-User-Role（信任头由网关注入）
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
