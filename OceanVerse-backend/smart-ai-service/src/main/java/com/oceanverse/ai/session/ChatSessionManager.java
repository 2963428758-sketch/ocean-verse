package com.oceanverse.ai.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanverse.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 多轮对话会话管理器
 * <p>
 * 基于 Redis List 存储对话历史，支持：
 * - 窗口裁剪：只保留最近 N 轮（由 AiProperties.historyRounds 配置）
 * - TTL 自动过期：30 分钟无活动后清除
 * - 容错设计：会话读写失败不影响主流程
 * <p>
 * 存储格式：每个 sessionId 对应一个 Redis List，
 * 每条记录是 JSON 序列化的 {userQuestion, aiAnswer} 消息对。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionManager {

    private static final String SESSION_KEY_PREFIX = "ai:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    /**
     * 保存用户提问和 AI 回答到会话历史
     *
     * @param sessionId    会话 ID
     * @param userQuestion 用户提问
     * @param aiAnswer     AI 完整回答
     */
    public void saveMessage(String sessionId, String userQuestion, String aiAnswer) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        String key = SESSION_KEY_PREFIX + sessionId;

        try {
            SessionMessage msg = new SessionMessage(userQuestion, aiAnswer);
            String json = objectMapper.writeValueAsString(msg);

            // 追加到 Redis List
            redisTemplate.opsForList().rightPush(key, json);

            // 只保留最近 N 轮（每轮 = 1 条 SessionMessage，包含 user + assistant）
            int maxRounds = aiProperties.getHistoryRounds();
            redisTemplate.opsForList().trim(key, -maxRounds, -1);

            // 刷新 TTL
            redisTemplate.expire(key, aiProperties.getSessionTtlMinutes(), TimeUnit.MINUTES);

            log.debug("会话消息已保存: sessionId={}, 当前轮数={}", sessionId,
                    redisTemplate.opsForList().size(key));
        } catch (Exception e) {
            log.warn("会话消息保存失败（不影响主流程）: {}", e.getMessage());
        }
    }

    /**
     * 获取会话历史，转为 Spring AI Message 列表
     * <p>
     * 返回格式：[UserMessage, AssistantMessage, UserMessage, AssistantMessage, ...]
     * 用于构造多轮对话上下文，让模型理解对话连贯性。
     *
     * @param sessionId 会话 ID
     * @return Message 列表（可能为空）
     */
    public List<Message> getHistoryMessages(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return new ArrayList<>();
        }

        String key = SESSION_KEY_PREFIX + sessionId;

        try {
            List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

            if (messages == null || messages.isEmpty()) {
                return new ArrayList<>();
            }

            List<Message> result = new ArrayList<>();
            for (String json : messages) {
                SessionMessage msg = objectMapper.readValue(json, SessionMessage.class);
                result.add(new UserMessage(msg.userQuestion()));
                result.add(new AssistantMessage(msg.aiAnswer()));
            }

            return result;
        } catch (Exception e) {
            log.warn("会话历史读取失败，降级为单轮对话: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 清空会话历史
     *
     * @param sessionId 会话 ID
     */
    public void clearSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        try {
            redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
            log.info("已清空会话历史: {}", sessionId);
        } catch (Exception e) {
            log.warn("会话历史清空失败: {}", e.getMessage());
        }
    }

    /**
     * 会话消息对（内部 DTO）
     * 每条记录包含一轮对话的用户提问和 AI 回答
     */
    private record SessionMessage(String userQuestion, String aiAnswer) {}
}
