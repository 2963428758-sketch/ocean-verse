package com.oceanverse.message.stream;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.message.config.RedisStreamConfig;
import com.oceanverse.message.dto.NotificationMessage;
import com.oceanverse.message.service.NotificationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通知消息消费者 — 监听 stream:notification
 * <p>
 * 收到通知消息后执行两步操作：
 * 1. 入库 sys_notification 表（持久化，支持历史查询和已读/未读标记）
 * 2. 通过 WebSocket 实时推送给目标用户
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStreamConsumer extends StreamConsumer {

    private final RedisStreamConfig streamConfig;
    private final NotificationService notificationService;

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
        String payload = fields.get(CommonConstants.FIELD_PAYLOAD);
        if (payload == null || payload.isBlank()) {
            log.warn("收到空通知消息，跳过处理");
            return;
        }

        try {
            NotificationMessage msg = parsePayload(payload, NotificationMessage.class);
            if (msg == null) {
                log.error("通知消息反序列化失败: payload={}", payload);
                return;
            }
            if (msg.getUserId() == null) {
                log.warn("通知消息缺少目标用户 ID，跳过: payload={}", payload);
                return;
            }

            // 入库 + WebSocket 推送
            notificationService.saveAndPush(msg);
            log.debug("通知消息处理完成: userId={}, type={}", msg.getUserId(), msg.getNotificationType());

        } catch (Exception e) {
            log.error("处理通知消息异常: payload={}", payload, e);
        }
    }
}
