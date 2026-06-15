package com.oceanverse.message.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oceanverse.message.config.RedisStreamConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Redis Stream 消费者抽象基类
 * <p>
 * 子类只需实现 onMessage 方法处理业务逻辑，
 * 消息确认 (ACK) 由框架自动完成。
 */
@Slf4j
public abstract class StreamConsumer {

    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 注册消费者到指定 Stream
     *
     * @param config       RedisStreamConfig
     * @param streamKey    Stream 标识
     * @param group        消费者组
     * @param consumerName 消费者名称
     */
    public void register(RedisStreamConfig config, String streamKey,
                         String group, String consumerName) {
        config.createContainer(streamKey, group, consumerName, fields -> {
            String type = fields.get("type");
            log.debug("消费消息: stream={}, type={}", streamKey, type);
            onMessage(fields, streamKey);
        });
    }

    /**
     * 处理消息 — 子类实现具体业务逻辑
     *
     * @param fields    消息字段
     * @param streamKey 来源 Stream
     */
    protected abstract void onMessage(Map<String, String> fields, String streamKey);

    /**
     * 将 payload 反序列化为指定类型
     */
    protected <T> T parsePayload(String payload, Class<T> clazz) {
        try {
            return MAPPER.readValue(payload, clazz);
        } catch (Exception e) {
            log.error("反序列化消息失败: type={}", clazz.getSimpleName(), e);
            return null;
        }
    }
}
