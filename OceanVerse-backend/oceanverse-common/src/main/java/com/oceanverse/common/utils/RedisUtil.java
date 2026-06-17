package com.oceanverse.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final StringRedisTemplate redisTemplate;

    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void set(String key, String value, long seconds) {
        set(key, value, seconds, TimeUnit.SECONDS);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 缓存 JSON 对象（使用 Jackson）
     */
    public void setObject(String key, Object obj, long seconds) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(obj);
            set(key, json, seconds);
        } catch (Exception e) {
            throw new RuntimeException("Redis 序列化失败", e);
        }
    }

    /**
     * 从 Redis 读取并反序列化 JSON 对象
     */
    public <T> T getObject(String key, Class<T> clazz) {
        String json = get(key);
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Redis Stream 操作方法 ====================

    /**
     * 向 Stream 添加一条消息
     * @param key Stream key
     * @param fields 消息体 (field-value pairs)
     * @return 消息 ID
     */
    public RecordId xAdd(String key, Map<String, String> fields) {
        return redisTemplate.opsForStream().add(key, fields);
    }

    /**
     * 向 Stream 添加一条消息，并限制 Stream 最大长度
     * @param key Stream key
     * @param fields 消息体
     * @param maxLen 最大长度（超过则修剪旧消息）
     * @return 消息 ID
     */
    public RecordId xAdd(String key, Map<String, String> fields, long maxLen) {
        RecordId recordId = redisTemplate.opsForStream().add(key, fields);
        redisTemplate.opsForStream().trim(key, maxLen);
        return recordId;
    }

    /**
     * 创建消费者组（已存在则忽略）
     * @param key Stream key
     * @param group 消费者组名
     */
    public void xGroupCreate(String key, String group) {
        try {
            redisTemplate.opsForStream().createGroup(key, group);
        } catch (Exception e) {
            // 消费者组已存在时忽略异常
        }
    }

    /**
     * 从 Stream 读取消息 (从指定偏移量开始)
     * @param offset 读取偏移量
     * @return 消息列表
     */
    public List<MapRecord<String, Object, Object>> xRead(StreamOffset<String> offset) {
        return redisTemplate.opsForStream().read(offset);
    }

    /**
     * 以消费者组身份读取消息
     * @param consumer 消费者标识
     * @param offset 读取偏移量
     * @return 消息列表
     */
    public List<MapRecord<String, Object, Object>> xReadGroup(
            Consumer consumer, StreamOffset<String> offset) {
        return redisTemplate.opsForStream().read(consumer, offset);
    }

    /**
     * 确认消息已被处理
     * @param key Stream key
     * @param group 消费者组
     * @param recordIds 消息 ID 列表
     * @return 确认成功的数量
     */
    public Long xAck(String key, String group, String... recordIds) {
        return redisTemplate.opsForStream().acknowledge(key, group, recordIds);
    }

    /**
     * 删除 Stream 中的消息
     * @param key Stream key
     * @param recordIds 消息 ID 列表
     * @return 删除数量
     */
    public Long xDel(String key, String... recordIds) {
        return redisTemplate.opsForStream().delete(key, recordIds);
    }

    /**
     * 获取 Stream 长度
     * @param key Stream key
     * @return Stream 中的消息数量
     */
    public Long xLen(String key) {
        return redisTemplate.opsForStream().size(key);
    }

    /**
     * 获取 Stream 挂起消息信息
     * @param key Stream key
     * @param group 消费者组
     * @return 挂起消息概要
     */
    public PendingMessagesSummary xPending(String key, String group) {
        return redisTemplate.opsForStream().pending(key, group);
    }
}
