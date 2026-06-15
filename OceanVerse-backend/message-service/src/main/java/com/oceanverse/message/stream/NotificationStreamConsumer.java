package com.oceanverse.message.stream;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.message.config.RedisStreamConfig;
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

        // TODO: 从 payload 中解析目标用户，通过 WebSocket 推送通知
        // 示例: NotificationMessage msg = parsePayload(payload, NotificationMessage.class);
        // NotificationWebSocket.sendToUser(msg.getUserId(), payload);
    }
}
