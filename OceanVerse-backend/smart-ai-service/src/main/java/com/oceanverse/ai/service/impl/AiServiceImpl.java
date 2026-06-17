package com.oceanverse.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanverse.ai.config.AiProperties;
import com.oceanverse.ai.mapper.ImageRecognitionMapper;
import com.oceanverse.ai.mapper.QaHistoryMapper;
import com.oceanverse.ai.service.AiService;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.utils.OssUtil;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.pojo.dto.ChatDTO;
import com.oceanverse.pojo.entity.ImageRecognition;
import com.oceanverse.pojo.entity.QaHistory;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import reactor.core.publisher.Flux;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * AI 智能服务实现 — 接入 DashScope 通义千问大模型
 * <p>
 * 核心能力：
 * 1. 图像识别 — 使用 qwen-vl-max 多模态视觉模型识别海洋生物
 * 2. 智能问答 — 使用 qwen-plus 对话模型 + SSE 流式响应
 * 3. 事件发布 — 通过 Redis Stream 发布 AI 事件到通知管道
 * <p>
 * 架构说明：
 * 注入 ChatModel（由 DashScope Starter 自动配置），按需构建 ChatClient。
 * 普通问答使用默认 qwen-plus，图像识别通过 DashScopeChatOptions 切换到 qwen-vl-max。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ImageRecognitionMapper recognitionMapper;
    private final QaHistoryMapper qaHistoryMapper;
    private final OssUtil ossUtil;
    private final AiProperties aiProperties;
    private final RedisUtil redisUtil;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    // ==================== System Prompt ====================

    private static final String CHAT_SYSTEM_PROMPT = """
            你是一位资深的海洋生物学家，拥有 20 年的海洋生态研究经验。
            你的专长包括：海洋生物分类学、珊瑚礁生态、海洋保护、深海生物学。

            回答要求：
            1. 专业准确，引用可靠知识
            2. 语言通俗易懂，适合大众理解
            3. 如果不确定，明确告知"这个问题我暂时没有确切答案"
            4. 回答控制在 300 字以内
            """;

    private static final String RECOGNITION_PROMPT = """
            请识别这张图片中的海洋生物，返回以下 JSON 格式（只返回 JSON，不要其他文字）：
            {
              "speciesName": "中文物种名",
              "scientificName": "拉丁学名",
              "confidence": 0.95,
              "description": "简要描述（50字以内）",
              "habitat": "主要栖息地",
              "conservationStatus": "保护等级"
            }
            """;

    // ==================== 图像识别（任务 1.2）====================

    @Override
    public Object recognizeImage(MultipartFile file, Double latitude, Double longitude) {
        long startTime = System.currentTimeMillis();

        // 1. 上传至 OSS
        String imageUrl = ossUtil.upload(file);

        // 2. 创建识别记录
        ImageRecognition record = new ImageRecognition();
        record.setRecognitionCode("REC" + UUID.randomUUID().toString().substring(0, 8));
        record.setImageUrl(imageUrl);
        record.setFileName(file.getOriginalFilename());
        record.setFileSize(file.getSize());
        record.setRecognitionType("AUTO");
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        try {
            // 3. 调用 qwen-vl-max 多模态视觉模型
            //    withMultiModel(true) 强制走多模态端点，.media(MimeType, URL) 传入图片
            URL imageUrlObj = new URL(imageUrl);
            MimeType imageMimeType = MimeTypeUtils.parseMimeType(
                    file.getContentType() != null ? file.getContentType() : "image/jpeg");
            String response = ChatClient.create(chatModel).prompt()
                    .options(DashScopeChatOptions.builder()
                            .withModel(aiProperties.getImageModel())
                            .withMultiModel(true)
                            .build())
                    .user(u -> u
                            .text(RECOGNITION_PROMPT)
                            .media(imageMimeType, imageUrlObj)
                    )
                    .call()
                    .content();

            log.info("视觉模型原始返回: {}", response);

            // 4. 解析模型返回的 JSON（兼容 markdown code block 包裹）
            String jsonContent = extractJson(response);
            JsonNode result = objectMapper.readTree(jsonContent);

            record.setPredictedSpeciesName(result.path("speciesName").asText("未知"));

            // 安全解析置信度：使用 asDouble 避免 NumberFormatException
            double confidenceValue = result.path("confidence").asDouble(0.50);
            record.setConfidenceScore(BigDecimal.valueOf(confidenceValue));
            record.setRecognitionResult(response);
            record.setAiModelVersion(aiProperties.getImageModel());

            // 5. 高置信度自动标记为已验证
            record.setVerificationStatus(confidenceValue > 0.8 ? "VERIFIED" : "PENDING");

        } catch (Exception e) {
            log.error("图像识别失败: {}", e.getMessage(), e);
            record.setAiModelVersion(aiProperties.getImageModel());
            record.setPredictedSpeciesName("识别失败");
            record.setConfidenceScore(BigDecimal.ZERO);
            record.setErrorMessage(e.getMessage());
            record.setVerificationStatus("PENDING");
        }

        // 6. 记录处理耗时并保存
        record.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
        record.setUserId(getCurrentUserId());
        recognitionMapper.insert(record);

        // 7. 发布识别完成事件到 Redis Stream（任务 1.4）
        publishRecognitionEvent(record);

        log.info("图像识别完成: code={}, species={}, confidence={}, 耗时={}ms",
                record.getRecognitionCode(), record.getPredictedSpeciesName(),
                record.getConfidenceScore(), record.getProcessingTimeMs());

        return record;
    }

    // ==================== 智能问答 SSE 流式（任务 1.3）====================

    @Override
    public Flux<String> chatStream(ChatDTO dto) {
        long startTime = System.currentTimeMillis();

        // 1. 创建问答记录骨架（流结束后填充完整回答）
        QaHistory history = new QaHistory();
        history.setQuestionCode("QA" + UUID.randomUUID().toString().substring(0, 8));
        history.setQuestionType(dto.getQuestionType() != null ? dto.getQuestionType() : "GENERAL");
        history.setQuestionText(dto.getQuestion());
        history.setSessionId(dto.getSessionId());
        history.setAiModelVersion(aiProperties.getChatModel());
        history.setUserId(getCurrentUserId());
        history.setCreateTime(LocalDateTime.now());
        history.setUpdateTime(LocalDateTime.now());
        history.setStatus(1);

        // 2. 构建流式 Flux —— 不在此处 subscribe，由 Controller 通过 SseEmitter 订阅
        StringBuilder fullAnswer = new StringBuilder();

        return ChatClient.create(chatModel).prompt()
                .system(CHAT_SYSTEM_PROMPT)
                .user(dto.getQuestion())
                .stream()
                .content()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .doOnNext(fullAnswer::append)
                .doOnComplete(() -> {
                    // 流结束：保存完整回答到数据库
                    history.setAnswerText(fullAnswer.toString());
                    history.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
                    try {
                        qaHistoryMapper.insert(history);
                        publishChatEvent(history);
                        log.info("智能问答完成: code={}, 耗时={}ms, 回答长度={}",
                                history.getQuestionCode(),
                                history.getProcessingTimeMs(),
                                fullAnswer.length());
                    } catch (Exception e) {
                        log.error("保存问答记录失败（不影响流式响应）", e);
                    }
                })
                .doOnError(e -> log.error("AI 流式调用异常", e));
    }

    // ==================== 历史记录查询 ====================

    @Override
    public Object getRecognitionHistory(Integer page, Integer size) {
        return recognitionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<ImageRecognition>()
                        .orderByDesc(ImageRecognition::getCreateTime)
        );
    }

    @Override
    public Object getChatHistory(Integer page, Integer size) {
        return qaHistoryMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<QaHistory>()
                        .orderByDesc(QaHistory::getCreateTime)
        );
    }

    // ==================== 问答反馈 + 事件发布（任务 1.5）====================

    @Override
    public void feedback(Long id, Integer feedback) {
        if (feedback == null || (feedback != 0 && feedback != 1)) {
            throw new IllegalArgumentException("反馈参数无效，仅支持 0（不满意）或 1（满意）");
        }
        QaHistory history = qaHistoryMapper.selectById(id);
        if (history != null) {
            history.setFeedback(feedback);
            history.setFeedbackText(feedback == 1 ? "满意" : "不满意");
            history.setUpdateTime(LocalDateTime.now());
            qaHistoryMapper.updateById(history);

            // 发布反馈事件到 Redis Stream
            publishFeedbackEvent(history);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前登录用户 ID（从 Spring Security 上下文提取）
     * <p>
     * 如果 Security 上下文中没有认证信息（匿名访问或测试场景），返回 null。
     */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long userId) {
                return userId;
            }
        } catch (Exception e) {
            log.debug("获取当前用户 ID 失败（可能为匿名访问）: {}", e.getMessage());
        }
        return null;
    }

    // ==================== Redis Stream 事件发布（任务 1.4）====================

    /**
     * 发布图像识别完成事件
     * <p>
     * 消息格式与 AiStreamConsumer 的消费协议一致：
     * fields = { "type": "AI", "payload": "{AiEvent JSON}" }
     */
    private void publishRecognitionEvent(ImageRecognition record) {
        try {
            Map<String, Object> event = Map.of(
                    "action", CommonConstants.ACTION_RECOGNITION_COMPLETE,
                    "userId", record.getUserId() != null ? record.getUserId() : 0L,
                    "recognitionCode", record.getRecognitionCode(),
                    "speciesName", record.getPredictedSpeciesName() != null
                            ? record.getPredictedSpeciesName() : "待确认",
                    "confidence", record.getConfidenceScore() != null
                            ? record.getConfidenceScore().doubleValue() : 0.0
            );

            redisUtil.xAdd(CommonConstants.STREAM_AI, Map.of(
                    "type", "AI",
                    "payload", objectMapper.writeValueAsString(event)
            ));

            log.info("已发布识别完成事件: {}", record.getRecognitionCode());
        } catch (Exception e) {
            log.error("发布识别事件失败（不影响主流程）: {}", e.getMessage());
        }
    }

    /**
     * 发布智能问答完成事件
     */
    private void publishChatEvent(QaHistory history) {
        try {
            Map<String, Object> event = Map.of(
                    "action", CommonConstants.ACTION_CHAT_SESSION,
                    "userId", history.getUserId() != null ? history.getUserId() : 0L,
                    "questionType", history.getQuestionType() != null
                            ? history.getQuestionType() : "GENERAL"
            );

            redisUtil.xAdd(CommonConstants.STREAM_AI, Map.of(
                    "type", "AI",
                    "payload", objectMapper.writeValueAsString(event)
            ));

            log.info("已发布问答完成事件: {}", history.getQuestionCode());
        } catch (Exception e) {
            log.error("发布问答事件失败（不影响主流程）: {}", e.getMessage());
        }
    }

    /**
     * 发布用户反馈事件
     */
    private void publishFeedbackEvent(QaHistory history) {
        try {
            Map<String, Object> event = Map.of(
                    "action", CommonConstants.ACTION_FEEDBACK,
                    "userId", history.getUserId() != null ? history.getUserId() : 0L,
                    "questionType", history.getQuestionType() != null
                            ? history.getQuestionType() : "GENERAL"
            );

            redisUtil.xAdd(CommonConstants.STREAM_AI, Map.of(
                    "type", "AI",
                    "payload", objectMapper.writeValueAsString(event)
            ));

            log.info("已发布反馈事件: questionCode={}", history.getQuestionCode());
        } catch (Exception e) {
            log.error("发布反馈事件失败（不影响主流程）: {}", e.getMessage());
        }
    }

    // ==================== JSON 解析工具 ====================

    /**
     * 从模型返回文本中提取 JSON 内容
     * <p>
     * 兼容以下格式：
     * - 纯 JSON：{"speciesName": "绿海龟", ...}
     * - Markdown 代码块：```json\n{"speciesName": "绿海龟", ...}\n```
     * - 前后有额外文字的混合内容
     */
    private String extractJson(String content) {
        if (content == null) {
            return "{}";
        }
        String trimmed = content.trim();

        // 尝试去除 markdown 代码块包裹
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }

        // 尝试直接解析
        if (trimmed.startsWith("{")) {
            return trimmed;
        }

        // 查找第一个 { 到最后一个 } 之间的内容
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }
}
