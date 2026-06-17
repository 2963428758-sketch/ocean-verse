package com.oceanverse.message.stream;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.message.config.RedisStreamConfig;
import com.oceanverse.message.dto.CommunityEvent;
import com.oceanverse.message.dto.NotificationMessage;
import com.oceanverse.message.service.NotificationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 社区事件消费者 — 监听 stream:community
 * <p>
 * 接收社区模块发布的领域事件（发帖、评论、点赞等），
 * 判断是否需要通知目标用户，若需要则构建 NotificationMessage
 * 并转发到 stream:notification 由 NotificationStreamConsumer 统一处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityStreamConsumer extends StreamConsumer {

    private final RedisStreamConfig streamConfig;
    private final NotificationService notificationService;

    @PostConstruct
    public void init() {
        register(
                streamConfig,
                CommonConstants.STREAM_COMMUNITY,
                CommonConstants.STREAM_CONSUMER_GROUP,
                CommonConstants.STREAM_CONSUMER_NAME + "-community"
        );
        log.info("社区事件消费者已启动");
    }

    @Override
    protected void onMessage(Map<String, String> fields, String streamKey) {
        String type = fields.get(CommonConstants.FIELD_TYPE);
        String payload = fields.get(CommonConstants.FIELD_PAYLOAD);

        if (payload == null || payload.isBlank()) {
            log.warn("收到空社区事件，跳过: type={}", type);
            return;
        }

        try {
            CommunityEvent event = parsePayload(payload, CommunityEvent.class);
            if (event == null) {
                log.error("社区事件反序列化失败: payload={}", payload);
                return;
            }

            log.info("收到社区事件: action={}, actorId={}, targetId={}",
                    event.getAction(), event.getActorId(), event.getTargetId());

            // 根据事件类型决定是否通知用户
            switch (event.getAction()) {
                case CommonConstants.ACTION_COMMENT_CREATED:
                    handleCommentCreated(event);
                    break;
                case CommonConstants.ACTION_LIKE_TOGGLED:
                    handleLikeToggled(event);
                    break;
                case CommonConstants.ACTION_POST_CREATED:
                    log.info("新动态已发布: actorId={}, postId={}", event.getActorId(), event.getTargetId());
                    break;
                case CommonConstants.ACTION_POST_DELETED:
                    log.info("动态已删除: actorId={}, postId={}", event.getActorId(), event.getTargetId());
                    break;
                case CommonConstants.ACTION_FAVORITE_TOGGLED:
                    log.info("收藏操作: actorId={}, targetId={}", event.getActorId(), event.getTargetId());
                    break;
                default:
                    log.warn("未知社区事件类型: action={}", event.getAction());
            }

        } catch (Exception e) {
            log.error("处理社区事件异常: type={}, payload={}", type, payload, e);
        }
    }

    /**
     * 处理评论事件 — 通知帖子作者
     */
    private void handleCommentCreated(CommunityEvent event) {
        // 不通知自己
        if (event.getActorId().equals(event.getTargetUserId())) {
            log.debug("用户评论了自己的帖子，跳过通知");
            return;
        }

        NotificationMessage msg = NotificationMessage.builder()
                .userId(event.getTargetUserId())
                .notificationType(CommonConstants.NOTIFY_COMMENT)
                .title("有人评论了你的动态")
                .content(event.getActorName() + "：「" + truncate(event.getContent(), 50) + "」")
                .senderId(event.getActorId())
                .senderName(event.getActorName())
                .relatedId(event.getTargetId())
                .build();

        notificationService.publishNotification(msg);
        log.info("评论通知已转发: 通知用户 {} 的帖子被评论", event.getTargetUserId());
    }

    /**
     * 处理点赞事件 — 通知帖子/评论作者
     */
    private void handleLikeToggled(CommunityEvent event) {
        // 不通知自己
        if (event.getActorId().equals(event.getTargetUserId())) {
            log.debug("用户点赞了自己的内容，跳过通知");
            return;
        }

        String targetTypeLabel = "POST".equals(event.getTargetType()) ? "动态" : "评论";

        NotificationMessage msg = NotificationMessage.builder()
                .userId(event.getTargetUserId())
                .notificationType(CommonConstants.NOTIFY_LIKE)
                .title("有人点赞了你的" + targetTypeLabel)
                .content(event.getActorName() + " 赞了你的" + targetTypeLabel)
                .senderId(event.getActorId())
                .senderName(event.getActorName())
                .relatedId(event.getTargetId())
                .build();

        notificationService.publishNotification(msg);
        log.info("点赞通知已转发: 通知用户 {} 的{}被点赞", event.getTargetUserId(), targetTypeLabel);
    }

    /**
     * 截断字符串，超过 maxLen 加省略号
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
