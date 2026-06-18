package com.oceanverse.message.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.message.dto.NotificationMessage;
import com.oceanverse.community.mapper.SysNotificationMapper;
import com.oceanverse.message.websocket.NotificationWebSocket;
import com.oceanverse.pojo.entity.SysNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知服务 — 入库持久化 + WebSocket 实时推送
 * <p>
 * 接收 NotificationMessage，执行两步操作：
 * 1. 入库 sys_notification 表（isRead=0）
 * 2. 通过 WebSocket 推送给在线用户
 * <p>
 * 同时提供 publishNotification 方法，将通知消息发布到 stream:notification，
 * 供 NotificationStreamConsumer 消费（解耦直接调用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SysNotificationMapper notificationMapper;
    private final RedisStreamService redisStreamService;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 持久化通知并通过 WebSocket 推送
     *
     * @param msg 通知消息
     */
    public void saveAndPush(NotificationMessage msg) {
        // 1. 入库
        SysNotification notification = new SysNotification();
        notification.setUserId(msg.getUserId());
        notification.setTitle(msg.getTitle());
        notification.setContent(msg.getContent());
        notification.setType(msg.getNotificationType());
        notification.setIsRead(0);
        notification.setRelatedId(msg.getRelatedId());
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        log.info("通知已入库: userId={}, type={}, title={}",
                msg.getUserId(), msg.getNotificationType(), msg.getTitle());

        // 2. WebSocket 推送
        try {
            String json = MAPPER.writeValueAsString(msg);
            NotificationWebSocket.sendToUser(msg.getUserId(), json);
            log.debug("WebSocket 推送完成: userId={}", msg.getUserId());
        } catch (Exception e) {
            log.warn("WebSocket 推送失败（不影响入库）: userId={}", msg.getUserId(), e);
        }
    }

    /**
     * 发布通知消息到 stream:notification
     * <p>
     * 供其他消费者（如 CommunityStreamConsumer）调用，
     * 将构建好的 NotificationMessage 发布到通知队列，
     * 由 NotificationStreamConsumer 统一消费。
     *
     * @param msg 通知消息
     */
    public void publishNotification(NotificationMessage msg) {
        redisStreamService.publish(CommonConstants.STREAM_NOTIFICATION, msg);
    }

    /**
     * 快速构建并发布一条通知
     *
     * @param userId           接收用户 ID
     * @param notificationType 通知类型
     * @param title            标题
     * @param content          内容
     * @param senderId         触发者 ID（可为 null）
     * @param senderName       触发者用户名（可为 null）
     * @param relatedId        关联业务 ID（可为 null）
     */
    public void publishNotification(Long userId, String notificationType,
                                    String title, String content,
                                    Long senderId, String senderName, Long relatedId) {
        NotificationMessage msg = NotificationMessage.builder()
                .userId(userId)
                .notificationType(notificationType)
                .title(title)
                .content(content)
                .senderId(senderId)
                .senderName(senderName)
                .relatedId(relatedId)
                .build();
        publishNotification(msg);
    }
}
