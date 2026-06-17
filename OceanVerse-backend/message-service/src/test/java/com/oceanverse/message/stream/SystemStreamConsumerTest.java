package com.oceanverse.message.stream;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.message.config.RedisStreamConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SystemStreamConsumer 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SystemStreamConsumerTest {

    @Mock
    private RedisStreamConfig streamConfig;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private SystemStreamConsumer consumer;

    private Map<String, String> buildFields(String type, String payload) {
        Map<String, String> fields = new HashMap<>();
        fields.put(CommonConstants.FIELD_TYPE, type);
        fields.put(CommonConstants.FIELD_PAYLOAD, payload);
        fields.put(CommonConstants.FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        return fields;
    }

    // ==================== 缓存刷新 ====================

    @Test
    @DisplayName("缓存刷新事件 → 调用 redisUtil.delete 删除缓存")
    void testCacheRefresh() {
        String payload = "{\"action\":\"CACHE_REFRESH\",\"targetKey\":\"species:cache:100\"}";

        consumer.onMessage(buildFields("CACHE_REFRESH", payload), CommonConstants.STREAM_SYSTEM);

        verify(redisUtil, times(1)).delete("species:cache:100");
    }

    @Test
    @DisplayName("缓存刷新（targetKey 为空）→ 不删除缓存")
    void testCacheRefresh_NullKey() {
        String payload = "{\"action\":\"CACHE_REFRESH\"}";

        consumer.onMessage(buildFields("CACHE_REFRESH", payload), CommonConstants.STREAM_SYSTEM);

        verify(redisUtil, never()).delete(anyString());
    }

    @Test
    @DisplayName("缓存刷新 delete 抛异常 → 不向上抛出")
    void testCacheRefresh_Exception() {
        when(redisUtil.delete("some:key")).thenThrow(new RuntimeException("Redis down"));
        String payload = "{\"action\":\"CACHE_REFRESH\",\"targetKey\":\"some:key\"}";

        assertDoesNotThrow(() ->
                consumer.onMessage(buildFields("CACHE_REFRESH", payload), CommonConstants.STREAM_SYSTEM));
    }

    // ==================== 用户注册 ====================

    @Test
    @DisplayName("用户注册事件 → 调用 redisUtil.increment 更新统计")
    void testUserRegister() {
        String payload = "{\"action\":\"USER_REGISTER\",\"targetKey\":\"user:100\",\"data\":\"newuser\"}";

        consumer.onMessage(buildFields("USER_REGISTER", payload), CommonConstants.STREAM_SYSTEM);

        verify(redisUtil, times(1)).increment("stats:total_users");
    }

    @Test
    @DisplayName("用户注册 increment 抛异常 → 不向上抛出")
    void testUserRegister_Exception() {
        when(redisUtil.increment("stats:total_users"))
                .thenThrow(new RuntimeException("Redis down"));
        String payload = "{\"action\":\"USER_REGISTER\",\"targetKey\":\"user:100\",\"data\":\"newuser\"}";

        assertDoesNotThrow(() ->
                consumer.onMessage(buildFields("USER_REGISTER", payload), CommonConstants.STREAM_SYSTEM));
    }

    // ==================== 统计更新 & 日志归档 ====================

    @Test
    @DisplayName("统计更新事件 → 仅日志，不操作 Redis")
    void testStatsUpdate() {
        String payload = "{\"action\":\"STATS_UPDATE\",\"targetKey\":\"dashboard\",\"data\":\"refresh\"}";

        consumer.onMessage(buildFields("STATS_UPDATE", payload), CommonConstants.STREAM_SYSTEM);

        verifyNoInteractions(redisUtil);
    }

    @Test
    @DisplayName("日志归档事件 → 仅日志，不操作 Redis")
    void testLogArchive() {
        String payload = "{\"action\":\"LOG_ARCHIVE\",\"targetKey\":\"log-2026-06\"}";

        consumer.onMessage(buildFields("LOG_ARCHIVE", payload), CommonConstants.STREAM_SYSTEM);

        verifyNoInteractions(redisUtil);
    }

    // ==================== 异常处理 ====================

    @Test
    @DisplayName("payload 为空 → 跳过处理")
    void testNullPayload() {
        Map<String, String> fields = new HashMap<>();
        fields.put(CommonConstants.FIELD_TYPE, "CACHE_REFRESH");
        consumer.onMessage(fields, CommonConstants.STREAM_SYSTEM);
        verifyNoInteractions(redisUtil);
    }

    @Test
    @DisplayName("非法 JSON → 不抛异常")
    void testInvalidJson() {
        assertDoesNotThrow(() ->
                consumer.onMessage(buildFields("CACHE_REFRESH", "not-json"),
                        CommonConstants.STREAM_SYSTEM));
    }

    @Test
    @DisplayName("未知事件类型 → 仅日志，不操作 Redis")
    void testUnknownAction() {
        String payload = "{\"action\":\"UNKNOWN_EVENT\",\"targetKey\":\"test\"}";
        consumer.onMessage(buildFields("UNKNOWN_EVENT", payload), CommonConstants.STREAM_SYSTEM);
        verifyNoInteractions(redisUtil);
    }
}
