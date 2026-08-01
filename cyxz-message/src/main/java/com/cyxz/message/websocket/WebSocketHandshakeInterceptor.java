package com.cyxz.message.websocket;

import com.cyxz.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器
 * <p>从连接 URL 的 query 参数中提取 token，用 JwtUtil 验证。
 * <p>验证通过后将 userId 存入 WebSocketSession 属性，供后续使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request.getURI());
        if (token == null || !jwtUtil.validateToken(token)) {
            log.warn("WebSocket 握手失败: token 无效");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        Long userId = jwtUtil.getUserId(token);
        attributes.put("userId", userId);
        log.info("WebSocket 握手成功: userId={}", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    /**
     * 从 URL query 参数提取 token
     */
    private String extractToken(URI uri) {
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if ("token".equals(kv[0]) && kv.length > 1) {
                return kv[1];
            }
        }
        return null;
    }
}
