package com.oceanverse.ai.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanverse.ai.config.AiProperties;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * AI API 调用限流器（任务 3.2）
 * <p>
 * 基于 Redis INCR + EXPIRE 实现每日调用次数限制。
 * 每个用户每天独立计数，次日 0 点后自动重置（key 2 天后过期）。
 * <p>
 * 限流策略（Fail-Closed）：
 * - 每日问答上限、识别上限均通过 AiProperties 配置
 * - userId 为 null 时拒绝请求（AI 接口必须携带 JWT）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiRateLimiter {

    private static final String RATE_LIMIT_KEY_PREFIX = "ai:ratelimit:";
    private static final String WARNED_KEY_PREFIX = "ai:ratelimit:warned:";

    private final StringRedisTemplate redisTemplate;
    private final AiProperties aiProperties;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    /**
     * 检查用户是否允许调用
     *
     * @param userId     用户 ID（不可为 null，AI 接口必须携带 JWT）
     * @param actionType "chat" 或 "recognition"
     * @return true = 允许调用，false = 已超限或身份异常
     */
    public boolean isAllowed(Long userId, String actionType) {
        if (userId == null) {
            log.warn("限流拒绝: userId 为 null，AI 接口要求登录");
            return false;
        }

        String key = buildKey(userId, actionType);
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        int limit = getLimit(actionType);

        if (count >= limit) {
            log.warn("用户 {} 已超过 {} 调用限制 ({}/{})", userId, actionType, count, limit);
            return false;
        }
        return true;
    }

    /**
     * 记录一次调用
     */
    public void recordCall(Long userId, String actionType) {
        if (userId == null) return;

        String key = buildKey(userId, actionType);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(2));

        checkAndWarnQuota(userId, actionType);
    }

    /**
     * 获取剩余可用次数
     *
     * @return 剩余次数，userId 为 null 时返回 0
     */
    public int getRemainingQuota(Long userId, String actionType) {
        if (userId == null) {
            return 0;
        }

        String key = buildKey(userId, actionType);
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        int limit = getLimit(actionType);
        return Math.max(0, limit - count);
    }

    private String buildKey(Long userId, String actionType) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return RATE_LIMIT_KEY_PREFIX + userId + ":" + actionType + ":" + dateStr;
    }

    private int getLimit(String actionType) {
        return "chat".equals(actionType)
                ? aiProperties.getDailyChatLimit()
                : aiProperties.getDailyRecognitionLimit();
    }

    private int getWarningThreshold(String actionType) {
        return "chat".equals(actionType)
                ? aiProperties.getChatWarningThreshold()
                : aiProperties.getRecognitionWarningThreshold();
    }

    /**
     * 检查剩余配额是否触达预警阈值，触达则发布通知到 stream:notification。
     * <p>
     * 使用 Redis warned key 保证每天每类只推一次，失败不影响主流程。
     */
    private void checkAndWarnQuota(Long userId, String actionType) {
        try {
            String key = buildKey(userId, actionType);
            String countStr = redisTemplate.opsForValue().get(key);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;
            int limit = getLimit(actionType);
            int remaining = limit - count;
            int threshold = getWarningThreshold(actionType);

            if (remaining > threshold || remaining <= 0) {
                return;
            }

            // 去重：当天已发过预警则跳过
            String warnedKey = buildWarnedKey(userId, actionType);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(warnedKey))) {
                return;
            }
            redisTemplate.opsForValue().set(warnedKey, "1", Duration.ofDays(2));

            // 构建通知
            String actionLabel = "chat".equals(actionType) ? "问答" : "识别";
            String title = "AI " + actionLabel + "次数即将用尽";
            String content = "今日剩余" + actionLabel + "次数：" + remaining + " 次，请合理安排使用。";

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("notificationType", CommonConstants.NOTIFY_QUOTA_WARNING);
            payload.put("title", title);
            payload.put("content", content);

            Map<String, String> fields = new HashMap<>();
            fields.put(CommonConstants.FIELD_TYPE, CommonConstants.NOTIFY_QUOTA_WARNING);
            fields.put(CommonConstants.FIELD_PAYLOAD, objectMapper.writeValueAsString(payload));
            redisUtil.xAdd(CommonConstants.STREAM_NOTIFICATION, fields);

            log.info("配额预警通知已发送: userId={}, actionType={}, remaining={}/{}", userId, actionType, remaining, limit);
        } catch (Exception e) {
            log.warn("配额预警通知发送失败（不影响主流程）: userId={}, actionType={}", userId, actionType, e);
        }
    }

    private String buildWarnedKey(Long userId, String actionType) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return WARNED_KEY_PREFIX + userId + ":" + actionType + ":" + dateStr;
    }
}
