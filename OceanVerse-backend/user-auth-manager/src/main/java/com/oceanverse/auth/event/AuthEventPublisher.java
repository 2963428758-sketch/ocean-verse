package com.oceanverse.auth.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证事件发布器 — 通过 Redis Stream 推送消息到 message-service
 * <p>
 * 消息格式与 message-service 的 StreamConsumer.onMessage() 对齐：
 * { type: "AUTH_xxx", payload: "{json}" }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventPublisher {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    public void publishForceLogout(Long userId, String username) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("reason", "管理员强制下线");
        payload.put("timestamp", LocalDateTime.now().toString());
        publish(CommonConstants.AUTH_FORCE_LOGOUT, payload);
    }

    public void publishLoginAlert(Long userId, String username, String currentIp, String previousIp) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("currentIp", currentIp);
        payload.put("previousIp", previousIp);
        payload.put("timestamp", LocalDateTime.now().toString());
        publish(CommonConstants.AUTH_LOGIN_ALERT, payload);
    }

    public void publishPasswordChange(Long userId, String username) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("message", "密码已修改，所有设备已强制登出");
        payload.put("timestamp", LocalDateTime.now().toString());
        publish(CommonConstants.AUTH_PASSWORD_CHANGE, payload);
    }

    private void publish(String type, Object payloadObj) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payloadObj);
            Map<String, String> fields = Map.of(
                    CommonConstants.FIELD_TYPE, type,
                    CommonConstants.FIELD_PAYLOAD, payloadJson
            );
            redisUtil.xAdd(CommonConstants.STREAM_NOTIFICATION, fields);
            log.info("Stream 消息已发送: type={}", type);
        } catch (Exception e) {
            log.warn("Stream 消息发送失败（不影响主流程）: type={}, error={}", type, e.getMessage());
        }
    }
}
