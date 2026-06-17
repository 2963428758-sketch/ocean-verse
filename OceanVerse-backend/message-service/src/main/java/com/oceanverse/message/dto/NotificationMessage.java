package com.oceanverse.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知消息 DTO — 用于 stream:notification
 * <p>
 * 由消费者从社区/AI事件转化而来，
 * 最终入库 SysNotification 并通过 WebSocket 推送给用户。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    /** 接收通知的用户 ID */
    private Long userId;

    /** 通知类型: LIKE / COMMENT / SYSTEM / AI_RESULT */
    private String notificationType;

    /** 通知标题，如"有人点赞了你的动态" */
    private String title;

    /** 通知内容摘要 */
    private String content;

    /** 触发者用户 ID（可为空） */
    private Long senderId;

    /** 触发者用户名（可为空） */
    private String senderName;

    /** 关联的业务 ID（帖子ID、识别记录ID等） */
    private Long relatedId;
}
