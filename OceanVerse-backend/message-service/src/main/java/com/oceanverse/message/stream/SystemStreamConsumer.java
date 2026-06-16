package com.oceanverse.message.stream;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.message.config.RedisStreamConfig;
import com.oceanverse.message.dto.SystemEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 系统消息消费者 — 监听 stream:system
 * <p>
 * 处理系统级消息：缓存刷新、日志记录、统计更新、用户注册事件等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemStreamConsumer extends StreamConsumer {

    private final RedisStreamConfig streamConfig;
    private final RedisUtil redisUtil;

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
        String type = fields.get(CommonConstants.FIELD_TYPE);
        String payload = fields.get(CommonConstants.FIELD_PAYLOAD);

        if (payload == null || payload.isBlank()) {
            log.warn("收到空系统消息，跳过: type={}", type);
            return;
        }

        try {
            SystemEvent event = parsePayload(payload, SystemEvent.class);
            if (event == null) {
                log.error("系统事件反序列化失败: payload={}", payload);
                return;
            }

            log.info("收到系统消息: action={}, targetKey={}", event.getAction(), event.getTargetKey());

            switch (event.getAction()) {
                case CommonConstants.ACTION_CACHE_REFRESH:
                    handleCacheRefresh(event);
                    break;
                case CommonConstants.ACTION_STATS_UPDATE:
                    handleStatsUpdate(event);
                    break;
                case CommonConstants.ACTION_LOG_ARCHIVE:
                    handleLogArchive(event);
                    break;
                case CommonConstants.ACTION_USER_REGISTER:
                    handleUserRegister(event);
                    break;
                default:
                    log.warn("未知系统事件类型: action={}", event.getAction());
            }

        } catch (Exception e) {
            log.error("处理系统消息异常: type={}, payload={}", type, payload, e);
        }
    }

    /**
     * 缓存刷新 — 删除指定的 Redis 缓存 key
     */
    private void handleCacheRefresh(SystemEvent event) {
        String key = event.getTargetKey();
        if (key == null || key.isBlank()) {
            log.warn("缓存刷新事件缺少 targetKey");
            return;
        }
        try {
            redisUtil.delete(key);
            log.info("缓存已刷新: key={}", key);
        } catch (Exception e) {
            log.error("缓存刷新失败: key={}", key, e);
        }
    }

    /**
     * 统计更新 — 记录日志，后续可扩展为更新统计缓存
     */
    private void handleStatsUpdate(SystemEvent event) {
        log.info("统计更新事件: targetKey={}, data={}", event.getTargetKey(), event.getData());
        // TODO: 可在此处触发 Dashboard 统计数据的预计算和缓存更新
    }

    /**
     * 日志归档 — 记录审计日志
     */
    private void handleLogArchive(SystemEvent event) {
        log.info("日志归档事件: targetKey={}, data={}", event.getTargetKey(), event.getData());
        // TODO: 可在此处执行日志文件的压缩归档操作
    }

    /**
     * 用户注册事件 — 记录并更新用户统计
     */
    private void handleUserRegister(SystemEvent event) {
        log.info("新用户注册: targetKey={}, username={}", event.getTargetKey(), event.getData());
        try {
            redisUtil.increment("stats:total_users");
            log.debug("用户统计计数器已更新");
        } catch (Exception e) {
            log.warn("更新用户统计计数器失败", e);
        }
    }
}
