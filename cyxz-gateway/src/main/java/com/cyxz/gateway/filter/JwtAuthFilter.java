package com.cyxz.gateway.filter;

import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.Result;
import com.cyxz.common.utils.TokenUtil;
import com.cyxz.auth.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 认证全局过滤器
 * <p>在每个请求进入 Gateway 时处理 JWT Token：
 * <ul>
 *   <li>白名单路径：有 Token 则解析注入 X-User-Id，无 Token 直接放行</li>
 *   <li>非白名单路径：必须携带有效 Token，否则返回 401</li>
 *   <li>OPTIONS 预检请求直接放行</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> WHITELIST = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/captcha/**",
            "/api/auth/refresh",
            "/api/post/list/**",
            "/api/circle/**"
    );

    /**
     * 执行 JWT 认证过滤
     * <p>对非白名单路径校验 Authorization 头中的 Bearer Token，
     * 验证通过后将 X-User-Id 注入请求头传递给下游服务。
     *
     * @param exchange 当前请求交换
     * @param chain    过滤器链
     * @return 异步处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        if (isWhitelisted(path)) {
            // 白名单路径：有 Token 就解析注入 X-User-Id，无 Token 直接放行
            String token = TokenUtil.extractBearerToken(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            if (token != null && jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserId(token);
                String role = jwtUtil.getRole(token);
                ServerHttpRequest mutatedRequest = request.mutate()
                        .headers(headers -> {
                            headers.remove("X-User-Id");
                            headers.set("X-User-Id", String.valueOf(userId));
                            headers.remove("X-User-Role");
                            headers.set("X-User-Role", role);
                        })
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
            return chain.filter(exchange);
        }

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String token = TokenUtil.extractBearerToken(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return unauthorized(response, ErrorCode.TOKEN_MISSING, "缺少Token");
        }

        if (!jwtUtil.validateToken(token)) {
            return unauthorized(response, ErrorCode.TOKEN_INVALID, "Token无效或已过期");
        }

        Long userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);
        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.set("X-User-Id", String.valueOf(userId));
                    headers.remove("X-User-Role");
                    headers.set("X-User-Role", role);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 全局过滤器执行顺序
     *
     * @return 优先级，-100 确保在路由转发前执行
     */
    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : WHITELIST) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, ErrorCode errorCode, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<Void> result = Result.fail(errorCode.getCode(), message);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":401,\"message\":\"未授权\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
