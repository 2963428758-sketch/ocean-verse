package com.oceanverse.ai.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanverse.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChatSessionManager 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChatSessionManagerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    private ObjectMapper objectMapper;
    private AiProperties aiProperties;
    private ChatSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        aiProperties = new AiProperties();
        aiProperties.setHistoryRounds(10);
        sessionManager = new ChatSessionManager(redisTemplate, objectMapper, aiProperties);
    }

    @Test
    void saveMessage_storesToRedisAndSetsTTL() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        sessionManager.saveMessage("session-1", "绿海龟吃什么？", "绿海龟主要以海草和海藻为食。");

        verify(listOperations).rightPush(eq("ai:session:session-1"), anyString());
        verify(listOperations).trim("ai:session:session-1", -10L, -1);
        verify(redisTemplate).expire("ai:session:session-1", 30, TimeUnit.MINUTES);
    }

    @Test
    void saveMessage_usesConfigurableTTL() {
        aiProperties.setSessionTtlMinutes(60L);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        sessionManager.saveMessage("session-2", "问题", "回答");

        verify(redisTemplate).expire("ai:session:session-2", 60, TimeUnit.MINUTES);
    }

    @Test
    void saveMessage_nullSessionId_doesNothing() {
        sessionManager.saveMessage(null, "question", "answer");
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void saveMessage_blankSessionId_doesNothing() {
        sessionManager.saveMessage("", "question", "answer");
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void saveMessage_exceptionSwallowed() {
        when(redisTemplate.opsForList()).thenThrow(new RuntimeException("Redis down"));
        // Should not throw
        assertDoesNotThrow(() -> sessionManager.saveMessage("session-1", "q", "a"));
    }

    @Test
    void getHistoryMessages_returnsCorrectMessagePairs() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        String json1 = "{\"userQuestion\":\"绿海龟吃什么？\",\"aiAnswer\":\"以海草为食\"}";
        String json2 = "{\"userQuestion\":\"它生活在哪里？\",\"aiAnswer\":\"热带和亚热带海域\"}";
        when(listOperations.range("ai:session:session-1", 0, -1))
                .thenReturn(Arrays.asList(json1, json2));

        List<Message> messages = sessionManager.getHistoryMessages("session-1");

        assertEquals(4, messages.size()); // 2 rounds × 2 messages each
        assertTrue(messages.get(0) instanceof UserMessage);
        assertTrue(messages.get(1) instanceof AssistantMessage);
        assertTrue(messages.get(2) instanceof UserMessage);
        assertTrue(messages.get(3) instanceof AssistantMessage);
        assertEquals("绿海龟吃什么？", messages.get(0).getText());
        assertEquals("以海草为食", messages.get(1).getText());
    }

    @Test
    void getHistoryMessages_nullSessionId_returnsEmpty() {
        List<Message> messages = sessionManager.getHistoryMessages(null);
        assertTrue(messages.isEmpty());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void getHistoryMessages_emptyList_returnsEmpty() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(List.of());

        List<Message> messages = sessionManager.getHistoryMessages("session-1");
        assertTrue(messages.isEmpty());
    }

    @Test
    void getHistoryMessages_exceptionSwallowed() {
        when(redisTemplate.opsForList()).thenThrow(new RuntimeException("Redis down"));
        List<Message> messages = sessionManager.getHistoryMessages("session-1");
        assertTrue(messages.isEmpty());
    }

    @Test
    void clearSession_deletesRedisKey() {
        when(redisTemplate.delete("ai:session:session-1")).thenReturn(true);
        sessionManager.clearSession("session-1");
        verify(redisTemplate).delete("ai:session:session-1");
    }

    @Test
    void clearSession_nullSessionId_doesNothing() {
        sessionManager.clearSession(null);
        verifyNoInteractions(redisTemplate);
    }
}
