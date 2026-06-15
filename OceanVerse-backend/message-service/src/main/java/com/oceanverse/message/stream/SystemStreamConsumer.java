package com.oceanverse.message.stream;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.message.config.RedisStreamConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 系统消息消费者 — 监听 stream:system
 * <p>
 * 处理系统级消息：日志记录、缓存刷新、统计更新等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemStreamConsumer extends StreamConsumer {

    private final RedisStreamConfig streamConfig;

    @PostConstruct
    public void init() {
        register(
                streamConfig,
                CommonConstants.STREAM_SYSTEM,
                CommonConstants.STREAM_CONSUMER_GROUP,
                CommonConstants.STREAM_CONSUMER_NAME + "-system"
        );
        log.info("系统消息消费者已启动");
    }

    @Override
    protected void onMessage(Map<String, String> fields, String streamKey) {
        String type = fields.get("type");
        String payload = fields.get("payload");
        log.info("收到系统消息: type={}, payload={}", type, payload);

        // TODO: 根据消息类型执行对应系统操作
        // 例如：缓存刷新、日志归档、数据统计等
    }
}
