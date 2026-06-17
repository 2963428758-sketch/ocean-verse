package com.oceanverse.message.stream;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.message.config.RedisStreamConfig;
import com.oceanverse.message.websocket.NotificationWebSocket;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通知消息消费者 — 监听 stream:notification
 * <p>
 * 处理系统通知类消息（点赞、评论、关注等），
 * 通过 WebSocket 推送给目标用户。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStreamConsumer extends StreamConsumer {

    private final RedisStreamConfig streamConfig;

    @PostConstruct
    public void init() {
        register(
                streamConfig,
                CommonConstants.STREAM_NOTIFICATION,
                CommonConstants.STREAM_CONSUMER_GROUP,
                CommonConstants.STREAM_CONSUMER_NAME + "-notification"
        );
        log.info("通知消息消费者已启动");
    }

    @Override
    protected void onMessage(Map<String, String> fields, String streamKey) {
        String type = fields.get("type");
        String payload = fields.get("payload");
        log.info("收到通知消息: type={}, payload={}", type, payload);

        try {
            Map<String, Object> payloadMap = MAPPER.readValue(payload, Map.class);
            Long userId = payloadMap.get("userId") != null
                    ? Long.valueOf(payloadMap.get("userId").toString()) : null;
            if (userId == null) {
                log.warn("通知消息缺少 userId: payload={}", payload);
                return;
            }
            String message = MAPPER.writeValueAsString(Map.of(
                    "type", type != null ? type : "",
                    "notificationId", payloadMap.getOrDefault("notificationId", ""),
                    "title", payloadMap.getOrDefault("title", ""),
                    "content", payloadMap.getOrDefault("content", ""),
                    "relatedId", payloadMap.getOrDefault("relatedId", ""),
                    "createTime", payloadMap.getOrDefault("createTime", "")
            ));
            NotificationWebSocket.sendToUser(userId, message);
            log.info("通知已推送: userId={}, type={}", userId, type);
        } catch (Exception e) {
            log.error("处理通知消息失败: type={}, payload={}", type, payload, e);
        }
    }
}
