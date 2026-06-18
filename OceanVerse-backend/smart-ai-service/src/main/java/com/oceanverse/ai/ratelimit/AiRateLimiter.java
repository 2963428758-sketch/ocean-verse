package com.oceanverse.ai.ratelimit;

import com.oceanverse.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    private final StringRedisTemplate redisTemplate;
    private final AiProperties aiProperties;

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
}
