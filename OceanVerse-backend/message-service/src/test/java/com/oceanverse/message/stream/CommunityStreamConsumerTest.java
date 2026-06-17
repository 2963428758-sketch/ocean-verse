package com.oceanverse.message.stream;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.message.config.RedisStreamConfig;
import com.oceanverse.message.dto.NotificationMessage;
import com.oceanverse.message.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CommunityStreamConsumer 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CommunityStreamConsumerTest {

    @Mock
    private RedisStreamConfig streamConfig;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommunityStreamConsumer consumer;

    private Map<String, String> buildFields(String type, String payload) {
        Map<String, String> fields = new HashMap<>();
        fields.put(CommonConstants.FIELD_TYPE, type);
        fields.put(CommonConstants.FIELD_PAYLOAD, payload);
        fields.put(CommonConstants.FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        return fields;
    }

    // ==================== 评论事件 ====================

    @Test
    @DisplayName("评论事件（不同用户）→ 转发通知")
    void testCommentCreated_DifferentUser() {
        String payload = "{\"action\":\"COMMENT_CREATED\",\"actorId\":1,\"actorName\":\"alice\"," +
                "\"targetUserId\":2,\"targetId\":10,\"targetType\":\"POST\",\"content\":\"好棒的文章！\"}";

        consumer.onMessage(buildFields("COMMENT_CREATED", payload), CommonConstants.STREAM_COMMUNITY);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationService, times(1)).publishNotification(captor.capture());

        NotificationMessage msg = captor.getValue();
        assertEquals(2L, msg.getUserId());
        assertEquals(CommonConstants.NOTIFY_COMMENT, msg.getNotificationType());
        assertEquals("有人评论了你的动态", msg.getTitle());
        assertTrue(msg.getContent().contains("alice"));
        assertEquals(1L, msg.getSenderId());
        assertEquals(10L, msg.getRelatedId());
    }

    @Test
    @DisplayName("评论事件（自己评论自己）→ 不通知")
    void testCommentCreated_SelfComment() {
        String payload = "{\"action\":\"COMMENT_CREATED\",\"actorId\":1,\"actorName\":\"alice\"," +
                "\"targetUserId\":1,\"targetId\":10,\"targetType\":\"POST\",\"content\":\"自己评论自己\"}";

        consumer.onMessage(buildFields("COMMENT_CREATED", payload), CommonConstants.STREAM_COMMUNITY);

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("评论内容超过50字 → 通知内容被截断加省略号")
    void testCommentCreated_LongContent() {
        String longContent = "这是一段非常非常长的评论内容，目的是确保字符数量超过五十个字的截断阈值，" +
                "从而验证 truncate 方法是否能正确地在指定长度处截断并添加省略号标记";
        String payload = "{\"action\":\"COMMENT_CREATED\",\"actorId\":1,\"actorName\":\"bob\"," +
                "\"targetUserId\":2,\"targetId\":10,\"targetType\":\"POST\",\"content\":\"" + longContent + "\"}";

        consumer.onMessage(buildFields("COMMENT_CREATED", payload), CommonConstants.STREAM_COMMUNITY);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationService, times(1)).publishNotification(captor.capture());
        // 通知格式: actorName：「截断内容...」，所以应检查是否包含 "..." 且内容长度被控制
        String content = captor.getValue().getContent();
        assertTrue(content.contains("..."), "长内容应被截断并包含省略号");
        assertTrue(content.endsWith("」"), "通知内容应以」结尾");
    }

    // ==================== 点赞事件 ====================

    @Test
    @DisplayName("点赞 POST 类型（不同用户）→ 转发通知")
    void testLikeToggled_Post_DifferentUser() {
        String payload = "{\"action\":\"LIKE_TOGGLED\",\"actorId\":3,\"actorName\":\"charlie\"," +
                "\"targetUserId\":2,\"targetId\":5,\"targetType\":\"POST\"}";

        consumer.onMessage(buildFields("LIKE_TOGGLED", payload), CommonConstants.STREAM_COMMUNITY);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationService, times(1)).publishNotification(captor.capture());

        NotificationMessage msg = captor.getValue();
        assertEquals(2L, msg.getUserId());
        assertEquals(CommonConstants.NOTIFY_LIKE, msg.getNotificationType());
        assertTrue(msg.getTitle().contains("动态"));
        assertTrue(msg.getContent().contains("charlie"));
    }

    @Test
    @DisplayName("点赞 COMMENT 类型 → 通知标题包含【评论】")
    void testLikeToggled_Comment() {
        String payload = "{\"action\":\"LIKE_TOGGLED\",\"actorId\":3,\"actorName\":\"dave\"," +
                "\"targetUserId\":2,\"targetId\":5,\"targetType\":\"COMMENT\"}";

        consumer.onMessage(buildFields("LIKE_TOGGLED", payload), CommonConstants.STREAM_COMMUNITY);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationService, times(1)).publishNotification(captor.capture());
        assertTrue(captor.getValue().getTitle().contains("评论"));
    }

    @Test
    @DisplayName("点赞自己 → 不通知")
    void testLikeToggled_Self() {
        String payload = "{\"action\":\"LIKE_TOGGLED\",\"actorId\":2,\"actorName\":\"self\"," +
                "\"targetUserId\":2,\"targetId\":5,\"targetType\":\"POST\"}";

        consumer.onMessage(buildFields("LIKE_TOGGLED", payload), CommonConstants.STREAM_COMMUNITY);
        verifyNoInteractions(notificationService);
    }

    // ==================== 不需要通知的事件 ====================

    @Test
    @DisplayName("发帖事件 → 仅日志，不通知")
    void testPostCreated() {
        String payload = "{\"action\":\"POST_CREATED\",\"actorId\":1,\"actorName\":\"alice\"," +
                "\"targetId\":100}";

        consumer.onMessage(buildFields("POST_CREATED", payload), CommonConstants.STREAM_COMMUNITY);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("删帖事件 → 仅日志，不通知")
    void testPostDeleted() {
        String payload = "{\"action\":\"POST_DELETED\",\"actorId\":1,\"actorName\":\"alice\"," +
                "\"targetId\":100}";

        consumer.onMessage(buildFields("POST_DELETED", payload), CommonConstants.STREAM_COMMUNITY);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("收藏事件 → 仅日志，不通知")
    void testFavoriteToggled() {
        String payload = "{\"action\":\"FAVORITE_TOGGLED\",\"actorId\":1,\"actorName\":\"alice\"," +
                "\"targetId\":100}";

        consumer.onMessage(buildFields("FAVORITE_TOGGLED", payload), CommonConstants.STREAM_COMMUNITY);
        verifyNoInteractions(notificationService);
    }

    // ==================== 异常处理 ====================

    @Test
    @DisplayName("payload 为空 → 跳过处理")
    void testNullPayload() {
        Map<String, String> fields = new HashMap<>();
        fields.put(CommonConstants.FIELD_TYPE, "COMMENT_CREATED");
        consumer.onMessage(fields, CommonConstants.STREAM_COMMUNITY);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("非法 JSON → 不抛异常")
    void testInvalidJson() {
        assertDoesNotThrow(() ->
                consumer.onMessage(buildFields("COMMENT_CREATED", "{bad-json}"),
                        CommonConstants.STREAM_COMMUNITY));
    }
}
