package com.cyxz.message.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器
 * <p>维护 userId → WebSocketSession 映射，支持按用户推送消息。
 * <p>每 30 秒发送 Ping 帧探测连接健康，失败则清理半开连接。
 * <p>单实例方案：本地内存 Map。多实例部署需改用 Redis pub/sub 广播。
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    private static final PingMessage PING = new PingMessage();

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void add(Long userId, WebSocketSession session) {
        sessions.put(userId, session);
        log.info("WebSocket 连接建立: userId={}, 当前在线: {}", userId, sessions.size());
    }

    /**
     * 移除会话（仅当 map 中存的正是该 session 时才移除，避免并发新连接被误删）
     */
    public void remove(Long userId, WebSocketSession session) {
        sessions.remove(userId, session);
        log.info("WebSocket 连接断开: userId={}, 当前在线: {}", userId, sessions.size());
    }

    public boolean isOnline(Long userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    public void sendToUser(Long userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.warn("推送消息失败: userId={}", userId, e);
            }
        }
    }

    /**
     * 心跳探测：每 30 秒向所有在线连接发送 Ping 帧
     * <p>浏览器收到 Ping 会自动回 Pong（无需 JS 代码）。
     * <p>发送失败说明连接已断，清理半开连接，触发前端 onclose → 自动重连。
     */
    @Scheduled(fixedRate = 30000)
    public void heartbeat() {
        if (sessions.isEmpty()) {
            return;
        }
        sessions.forEach((userId, session) -> {
            if (!session.isOpen()) {
                sessions.remove(userId, session);
                return;
            }
            try {
                session.sendMessage(PING);
            } catch (Exception e) {
                log.info("心跳失败，清理连接: userId={}", userId);
                sessions.remove(userId, session);
                try {
                    session.close();
                } catch (Exception ignored) {
                }
            }
        });
    }
}
