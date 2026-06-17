package com.oceanverse.message.stream;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.message.config.RedisStreamConfig;
import com.oceanverse.message.dto.NotificationMessage;
import com.oceanverse.message.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
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
 * NotificationStreamConsumer 单元测试
 */
@ExtendWith(MockitoExtension.class)
class NotificationStreamConsumerTest {

    @Mock
    private RedisStreamConfig streamConfig;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationStreamConsumer consumer;

    private static final String VALID_PAYLOAD =
            "{\"userId\":1,\"notificationType\":\"LIKE\",\"title\":\"有人点赞了你的动态\"," +
            "\"content\":\"researcher 赞了你的动态\",\"senderId\":2,\"senderName\":\"researcher\",\"relatedId\":5}";

    private Map<String, String> buildFields(String payload) {
        Map<String, String> fields = new HashMap<>();
        fields.put(CommonConstants.FIELD_TYPE, "LIKE");
        fields.put(CommonConstants.FIELD_PAYLOAD, payload);
        fields.put(CommonConstants.FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        return fields;
    }

    @Test
    @DisplayName("正常通知消息 → 调用 saveAndPush 入库+推送")
    void testValidNotification() {
        consumer.onMessage(buildFields(VALID_PAYLOAD), CommonConstants.STREAM_NOTIFICATION);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationService, times(1)).saveAndPush(captor.capture());

        NotificationMessage msg = captor.getValue();
        assertEquals(1L, msg.getUserId());
        assertEquals("LIKE", msg.getNotificationType());
        assertEquals("有人点赞了你的动态", msg.getTitle());
        assertEquals(2L, msg.getSenderId());
        assertEquals("researcher", msg.getSenderName());
        assertEquals(5L, msg.getRelatedId());
    }

    @Test
    @DisplayName("payload 为空 → 跳过处理，不调用 saveAndPush")
    void testNullPayload() {
        Map<String, String> fields = new HashMap<>();
        fields.put(CommonConstants.FIELD_TYPE, "LIKE");
        // payload 不存在

        consumer.onMessage(fields, CommonConstants.STREAM_NOTIFICATION);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("payload 为空字符串 → 跳过处理")
    void testBlankPayload() {
        consumer.onMessage(buildFields(""), CommonConstants.STREAM_NOTIFICATION);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("payload 非法 JSON → 不抛异常，不调用 saveAndPush")
    void testInvalidJson() {
        consumer.onMessage(buildFields("not-a-json"), CommonConstants.STREAM_NOTIFICATION);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("userId 为 null → 跳过处理")
    void testMissingUserId() {
        String payload = "{\"notificationType\":\"LIKE\",\"title\":\"test\"}";
        consumer.onMessage(buildFields(payload), CommonConstants.STREAM_NOTIFICATION);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("saveAndPush 抛异常 → 被 catch 不向上抛出")
    void testSaveAndPushException() {
        doThrow(new RuntimeException("DB error"))
                .when(notificationService).saveAndPush(any());

        // 不应抛出异常
        assertDoesNotThrow(() ->
                consumer.onMessage(buildFields(VALID_PAYLOAD), CommonConstants.STREAM_NOTIFICATION));
    }
}
