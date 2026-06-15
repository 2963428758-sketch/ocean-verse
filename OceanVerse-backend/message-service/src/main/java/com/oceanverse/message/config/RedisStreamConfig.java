package com.oceanverse.message.config;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.utils.RedisUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 配置 — 消息监听容器
 * <p>
 * 使用 Spring Data Redis 的 StreamMessageListenerContainer
 * 实现异步消息消费，替代传统 MQ。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfig {

    private final RedisConnectionFactory connectionFactory;
    private final RedisUtil redisUtil;

    private final List<StreamMessageListenerContainer<String, MapRecord<String, String, String>>> containers = new ArrayList<>();

    /**
     * 创建 Stream 消息监听容器
     */
    @SuppressWarnings("unchecked")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> createContainer(
            String streamKey, String group, String consumerName,
            java.util.function.Consumer<Map<String, String>> handler) {

        // 确保消费者组存在
        redisUtil.xGroupCreate(streamKey, group);

        var options = StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(2))
                .batchSize(10)
                .build();

        var container = StreamMessageListenerContainer
                .create(connectionFactory, options);
        container.start();

        container.receive(
                Consumer.from(group, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                (StreamListener<String, MapRecord<String, String, String>>) message -> {
                    try {
                        log.debug("收到 Stream 消息: stream={}, id={}", streamKey, message.getId());
                        handler.accept(message.getValue());
                        redisUtil.xAck(streamKey, group, message.getId().getValue());
                    } catch (Exception e) {
                        log.error("处理 Stream 消息异常: stream={}, id={}", streamKey, message.getId(), e);
                    }
                });

        containers.add(container);
        log.info("Stream 消费者已注册: stream={}, group={}, consumer={}", streamKey, group, consumerName);
        return container;
    }

    /**
     * 初始化默认消费者组
     */
    @PostConstruct
    public void initConsumerGroups() {
        String group = CommonConstants.STREAM_CONSUMER_GROUP;
        redisUtil.xGroupCreate(CommonConstants.STREAM_NOTIFICATION, group);
        redisUtil.xGroupCreate(CommonConstants.STREAM_COMMUNITY, group);
        redisUtil.xGroupCreate(CommonConstants.STREAM_AI, group);
        redisUtil.xGroupCreate(CommonConstants.STREAM_SYSTEM, group);
        log.info("Redis Stream 消费者组已初始化");
    }

    @PreDestroy
    public void destroy() {
        containers.forEach(StreamMessageListenerContainer::stop);
        log.info("Redis Stream 监听容器已停止");
    }
}
