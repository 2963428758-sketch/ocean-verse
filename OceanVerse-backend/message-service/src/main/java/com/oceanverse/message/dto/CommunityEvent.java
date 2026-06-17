package com.oceanverse.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社区事件 DTO — 用于 stream:community
 * <p>
 * 社区模块在发帖、评论、点赞等操作后发布此事件，
 * 由 CommunityStreamConsumer 消费并决定是否需要通知用户。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityEvent {

    /** 事件动作: POST_CREATED / COMMENT_CREATED / LIKE_TOGGLED 等 */
    private String action;

    /** 操作者用户 ID */
    private Long actorId;

    /** 操作者用户名 */
    private String actorName;

    /** 被通知的目标用户 ID（如帖子作者） */
    private Long targetUserId;

    /** 关联业务 ID（帖子ID、评论ID） */
    private Long targetId;

    /** 目标类型: POST / COMMENT（用于点赞/收藏多态场景） */
    private String targetType;

    /** 内容摘要（如评论前50字） */
    private String content;
}
