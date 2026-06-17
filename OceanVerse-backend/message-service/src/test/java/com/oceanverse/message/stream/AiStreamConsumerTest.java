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
 * AiStreamConsumer 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AiStreamConsumerTest {

    @Mock
    private RedisStreamConfig streamConfig;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AiStreamConsumer consumer;

    private Map<String, String> buildFields(String type, String payload) {
        Map<String, String> fields = new HashMap<>();
        fields.put(CommonConstants.FIELD_TYPE, type);
        fields.put(CommonConstants.FIELD_PAYLOAD, payload);
        fields.put(CommonConstants.FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        return fields;
    }

    // ==================== 识别完成事件 ====================

    @Test
    @DisplayName("识别完成事件 → 转发通知，包含物种名和置信度百分比")
    void testRecognitionComplete() {
        String payload = "{\"action\":\"RECOGNITION_COMPLETE\",\"userId\":5," +
                "\"recognitionCode\":\"REC-001\",\"speciesName\":\"蓝鳍金枪鱼\",\"confidence\":0.92}";

        consumer.onMessage(buildFields("RECOGNITION_COMPLETE", payload), CommonConstants.STREAM_AI);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationService, times(1)).publishNotification(captor.capture());

        NotificationMessage msg = captor.getValue();
        assertEquals(5L, msg.getUserId());
        assertEquals(CommonConstants.NOTIFY_AI_RESULT, msg.getNotificationType());
        assertEquals("图像识别已完成", msg.getTitle());
        assertTrue(msg.getContent().contains("蓝鳍金枪鱼"));
        assertTrue(msg.getContent().contains("92.0%"));
    }

    @Test
    @DisplayName("识别完成（置信度为 null）→ 显示【未知】")
    void testRecognitionComplete_NullConfidence() {
        String payload = "{\"action\":\"RECOGNITION_COMPLETE\",\"userId\":5," +
                "\"speciesName\":\"海豚\"}";

        consumer.onMessage(buildFields("RECOGNITION_COMPLETE", payload), CommonConstants.STREAM_AI);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationService, times(1)).publishNotification(captor.capture());
        assertTrue(captor.getValue().getContent().contains("未知"));
    }

    @Test
    @DisplayName("识别完成（物种名为 null）→ 显示【待确认】")
    void testRecognitionComplete_NullSpecies() {
        String payload = "{\"action\":\"RECOGNITION_COMPLETE\",\"userId\":5,\"confidence\":0.5}";

        consumer.onMessage(buildFields("RECOGNITION_COMPLETE", payload), CommonConstants.STREAM_AI);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationService, times(1)).publishNotification(captor.capture());
        assertTrue(captor.getValue().getContent().contains("待确认"));
    }

    @Test
    @DisplayName("识别完成（userId 为 null）→ 不通知")
    void testRecognitionComplete_NullUserId() {
        String payload = "{\"action\":\"RECOGNITION_COMPLETE\",\"speciesName\":\"鲨鱼\",\"confidence\":0.8}";

        consumer.onMessage(buildFields("RECOGNITION_COMPLETE", payload), CommonConstants.STREAM_AI);
        verifyNoInteractions(notificationService);
    }

    // ==================== 其他 AI 事件 ====================

    @Test
    @DisplayName("问答会话事件 → 仅日志，不通知")
    void testChatSession() {
        String payload = "{\"action\":\"CHAT_SESSION\",\"userId\":5,\"questionType\":\"GENERAL\"}";

        consumer.onMessage(buildFields("CHAT_SESSION", payload), CommonConstants.STREAM_AI);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("用户反馈事件 → 仅日志，不通知")
    void testFeedback() {
        String payload = "{\"action\":\"FEEDBACK\",\"userId\":5}";

        consumer.onMessage(buildFields("FEEDBACK", payload), CommonConstants.STREAM_AI);
        verifyNoInteractions(notificationService);
    }

    // ==================== 异常处理 ====================

    @Test
    @DisplayName("payload 为空 → 跳过处理")
    void testNullPayload() {
        Map<String, String> fields = new HashMap<>();
        fields.put(CommonConstants.FIELD_TYPE, "RECOGNITION_COMPLETE");
        consumer.onMessage(fields, CommonConstants.STREAM_AI);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("非法 JSON → 不抛异常")
    void testInvalidJson() {
        assertDoesNotThrow(() ->
                consumer.onMessage(buildFields("RECOGNITION_COMPLETE", "{{bad}}"),
                        CommonConstants.STREAM_AI));
    }
}
