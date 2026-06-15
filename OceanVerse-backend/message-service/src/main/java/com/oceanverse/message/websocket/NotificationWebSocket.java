package com.oceanverse.message.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 消息推送服务
 */
@Slf4j
@Component
@ServerEndpoint("/ws/notification/{userId}")
public class NotificationWebSocket {

    private static final ConcurrentHashMap<Long, Session> SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        SESSIONS.put(userId, session);
        log.info("WebSocket 连接建立: userId={}", userId);
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        SESSIONS.remove(userId);
        log.info("WebSocket 连接关闭: userId={}", userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 异常: ", error);
    }

    /**
     * 向指定用户发送消息
     */
    public static void sendToUser(Long userId, String message) {
        Session session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("WebSocket 发送失败: userId={}", userId, e);
            }
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public static void broadcast(String message) {
        SESSIONS.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.error("WebSocket 广播失败", e);
                }
            }
        });
    }

    public static int getOnlineCount() {
        return SESSIONS.size();
    }
}
