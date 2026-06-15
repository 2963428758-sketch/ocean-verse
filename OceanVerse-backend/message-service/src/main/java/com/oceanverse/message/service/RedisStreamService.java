package com.oceanverse.message.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oceanverse.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Stream 消息服务 — 替代传统 MQ
 * <p>
 * 使用 Redis Stream 数据结构实现消息的发布与消费，
 * 支持消费者组模式，保证消息不丢失。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamService {

    private final RedisUtil redisUtil;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final long DEFAULT_MAX_LEN = 10000;

    /**
     * 发布消息到指定 Stream
     *
     * @param streamKey Stream 标识
     * @param message   消息对象（将序列化为 JSON）
     * @return 消息 ID
     */
    public RecordId publish(String streamKey, Object message) {
        try {
            String json = MAPPER.writeValueAsString(message);
            Map<String, String> fields = new HashMap<>();
            fields.put("type", message.getClass().getSimpleName());
            fields.put("payload", json);
            fields.put("timestamp", String.valueOf(System.currentTimeMillis()));

            RecordId recordId = redisUtil.xAdd(streamKey, fields, DEFAULT_MAX_LEN);
            log.debug("Stream 消息已发布: stream={}, id={}", streamKey, recordId);
            return recordId;
        } catch (Exception e) {
            log.error("发布 Stream 消息失败: stream={}", streamKey, e);
            throw new RuntimeException("发布 Stream 消息失败", e);
        }
    }

    /**
     * 发布原始字段消息到指定 Stream
     *
     * @param streamKey Stream 标识
     * @param fields    消息字段
     * @return 消息 ID
     */
    public RecordId publishRaw(String streamKey, Map<String, String> fields) {
        RecordId recordId = redisUtil.xAdd(streamKey, fields, DEFAULT_MAX_LEN);
        log.debug("Stream 原始消息已发布: stream={}, id={}", streamKey, recordId);
        return recordId;
    }

    /**
     * 确保消费者组存在
     */
    public void ensureGroup(String streamKey, String group) {
        redisUtil.xGroupCreate(streamKey, group);
    }
}
