package com.oceanverse.ai.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanverse.ai.config.AiProperties;
import com.oceanverse.common.utils.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiRateLimiter 单元测试（Fail-Closed 升级版）
 */
@ExtendWith(MockitoExtension.class)
class AiRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private ObjectMapper objectMapper;

    private AiProperties aiProperties;
    private AiRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        // dailyChatLimit=50, dailyRecognitionLimit=10 (defaults)
        rateLimiter = new AiRateLimiter(redisTemplate, aiProperties, redisUtil, objectMapper);
    }

    @Test
    void isAllowed_underLimit_returnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("10");

        assertTrue(rateLimiter.isAllowed(1L, "chat"));
    }

    @Test
    void isAllowed_overLimit_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("50");

        assertFalse(rateLimiter.isAllowed(1L, "chat"));
    }

    @Test
    void isAllowed_noPriorCalls_returnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        assertTrue(rateLimiter.isAllowed(1L, "chat"));
    }

    @Test
    void isAllowed_nullUserId_rejected() {
        // Fail-Closed: userId 为 null 时拒绝请求（AI 接口必须携带 JWT）
        assertFalse(rateLimiter.isAllowed(null, "chat"));
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void isAllowed_recognitionType_usesLowerLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("10");

        // 识别上限 10，10 >= 10 应被拒绝
        assertFalse(rateLimiter.isAllowed(1L, "recognition"));
    }

    @Test
    void isAllowed_recognitionType_underLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("9");

        assertTrue(rateLimiter.isAllowed(1L, "recognition"));
    }

    @Test
    void recordCall_incrementsRedisCounter() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        rateLimiter.recordCall(1L, "chat");

        verify(valueOperations).increment(anyString());
        verify(redisTemplate).expire(anyString(), eq(Duration.ofDays(2)));
    }

    @Test
    void recordCall_nullUserId_skipped() {
        rateLimiter.recordCall(null, "chat");
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void getRemainingQuota_calculatesCorrectly() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("20");

        int remaining = rateLimiter.getRemainingQuota(1L, "chat");

        assertEquals(30, remaining);  // 50 - 20 = 30
    }

    @Test
    void getRemainingQuota_nullUserId_returnsZero() {
        int remaining = rateLimiter.getRemainingQuota(null, "chat");
        assertEquals(0, remaining);
    }

    @Test
    void getRemainingQuota_overLimit_returnsZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("60");

        int remaining = rateLimiter.getRemainingQuota(1L, "chat");

        assertEquals(0, remaining);  // max(0, 50 - 60) = 0
    }

    @Test
    void getRemainingQuota_noPriorCalls_returnsFullQuota() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        int remaining = rateLimiter.getRemainingQuota(1L, "recognition");

        assertEquals(10, remaining);  // 识别上限 10
    }

    @Test
    void isAllowed_respectsConfigurableLimit() {
        aiProperties.setDailyChatLimit(5);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("5");

        // 配置上限 5，当前 5 >= 5 应被拒绝
        assertFalse(rateLimiter.isAllowed(1L, "chat"));
    }
}
