package com.oceanverse.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanverse.ai.cache.SemanticCacheService;
import com.oceanverse.ai.config.AiProperties;
import com.oceanverse.ai.mapper.ImageRecognitionMapper;
import com.oceanverse.ai.mapper.QaHistoryMapper;
import com.oceanverse.ai.prompt.PromptTemplateManager;
import com.oceanverse.ai.rag.KnowledgeBaseService;
import com.oceanverse.ai.service.AiService;
import com.oceanverse.ai.session.ChatSessionManager;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.utils.OssUtil;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.pojo.dto.AiObservationDTO;
import com.oceanverse.pojo.dto.ChatDTO;
import com.oceanverse.pojo.entity.ImageRecognition;
import com.oceanverse.pojo.entity.Observation;
import com.oceanverse.pojo.entity.QaHistory;
import com.oceanverse.pojo.entity.Species;
import com.oceanverse.species.mapper.SpeciesMapper;
import com.oceanverse.eco.mapper.ObservationMapper;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 智能服务实现 — 接入 DashScope 通义千问大模型
 * <p>
 * 核心能力：
 * 1. 图像识别 — 使用 qwen-vl-max 多模态视觉模型识别海洋生物
 * 2. 智能问答 — 使用 qwen-plus 对话模型 + SSE 流式响应 + RAG 检索增强 + 多轮对话
 * 3. 事件发布 — 通过 Redis Stream 发布 AI 事件到通知管道
 * <p>
 * 架构说明：
 * 注入 ChatModel（由 DashScope Starter 自动配置），按需构建 ChatClient。
 * 普通问答使用 qwen-plus，图像识别通过 DashScopeChatOptions 切换到 qwen-vl-max。
 * <p>
 * 第二层能力（技术深度）：
 * - PromptTemplateManager: 按 questionType 分发差异化 System Prompt
 * - KnowledgeBaseService: RAG 检索增强，从 species 知识库中检索相关上下文
 * - ChatSessionManager: Redis 多轮对话上下文管理
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

    // 第二层新增依赖
    private final PromptTemplateManager promptTemplateManager;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatSessionManager chatSessionManager;

    // 第三层新增依赖
    private final SemanticCacheService semanticCacheService;
    private final SpeciesMapper speciesMapper;
    private final ObservationMapper observationMapper;

    // ==================== System Prompt ====================

    // CHAT_SYSTEM_PROMPT 已迁移至 PromptTemplateManager，按 questionType 分发差异化 Prompt

    private static final String RECOGNITION_PROMPT = """
            请识别这张图片中的海洋生物，返回以下 JSON 格式（只返回 JSON，不要其他文字）：
            {
              "speciesName": "中文物种名（无法识别时写\\"未知\\"）",
              "scientificName": "拉丁学名",
              "commonName": "英文通用名",
              "confidence": "识别的置信度（0.0~1.0 之间，图片清晰且特征明显时偏高，模糊或特征不明时偏低）",
              "description": "简要描述（50字以内）",
              "morphology": "形态特征描述（100字以内，包括体型、颜色、显著特征等）",
              "ecology": "生态习性（食性、活动规律、社会行为等，100字以内）",
              "habitat": "主要栖息地",
              "kingdom": "界（如 Animalia）",
              "phylum": "门（如 Chordata）",
              "className": "纲（如 Mammalia）",
              "orderName": "目（如 Sirenia）",
              "family": "科（如 Dugongidae）",
              "genus": "属（如 Dugong）",
              "species": "种加词（如 dugon）",
              "iucnStatus": "IUCN等级（EX/EW/CR/EN/VU/NT/LC 之一，不确定写 null）",
              "protectionLevel": "中国保护等级（1=一级,2=二级,3=三级,不确定写 null）",
              "isEndemic": "是否特有种（0或1，不确定写0）",
              "isInvasive": "是否入侵物种（0或1，不确定写0）"
            }
            """;;

    /** 默认置信度（模型未返回置信度时的回退值） */
    private static final double DEFAULT_CONFIDENCE = 0.50;
    /** 高置信度阈值 — 超过此值自动标记为 VERIFIED */
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    /** 缓存流式回答的每个 chunk 字符数 */
    private static final int STREAM_CHUNK_SIZE = 10;
    /** RAG 检索返回的 Top-K 文档数 */
    private static final int RAG_TOP_K = 3;

    // ==================== 图像识别（任务 1.2）====================

    @Override
    public Map<String, Object> recognizeImage(MultipartFile file, Double latitude, Double longitude) {
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

        JsonNode parsed = null;

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
            parsed = objectMapper.readTree(jsonContent);

            record.setPredictedSpeciesName(parsed.path("speciesName").asText("未知"));

            // 安全解析置信度：使用 asDouble 避免 NumberFormatException
            double confidenceValue = parsed.path("confidence").asDouble(DEFAULT_CONFIDENCE);
            record.setConfidenceScore(BigDecimal.valueOf(confidenceValue));
            record.setRecognitionResult(response);
            record.setAiModelVersion(aiProperties.getImageModel());

            // 5. 高置信度自动标记为已验证
            record.setVerificationStatus(confidenceValue > HIGH_CONFIDENCE_THRESHOLD ? "VERIFIED" : "PENDING");

        } catch (Exception e) {
            log.error("图像识别失败: {}", e.getMessage(), e);
            record.setAiModelVersion(aiProperties.getImageModel());
            record.setPredictedSpeciesName("识别失败");
            record.setConfidenceScore(BigDecimal.ZERO);
            record.setErrorMessage("图像识别处理失败，请稍后重试");
            record.setVerificationStatus("PENDING");
        }

        // 6. 记录处理耗时并保存
        record.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
        record.setUserId(getCurrentUserId());
        recognitionMapper.insert(record);

        // 7. 发布识别完成事件到 Redis Stream（任务 1.4）
        publishRecognitionEvent(record);

        // 7.5 识别成功后关联物种表 — 三层递进匹配
        Map<String, Object> result = null;
        if (record.getPredictedSpeciesName() != null && !"识别失败".equals(record.getPredictedSpeciesName())) {
            String predictedName = record.getPredictedSpeciesName();

            // Tier 1: 精确匹配（commonName / scientificName / chineseName）
            Species species = speciesMapper.selectOne(
                    new LambdaQueryWrapper<Species>()
                            .eq(Species::getCommonName, predictedName)
                            .or()
                            .eq(Species::getScientificName, predictedName)
                            .or()
                            .eq(Species::getChineseName, predictedName)
                            .last("LIMIT 1")
            );

            // Tier 2: 模糊包含匹配（双向 LIKE，要求名称长度 ≥ 2）
            if (species == null && predictedName.length() >= 2) {
                species = speciesMapper.selectOne(
                        new LambdaQueryWrapper<Species>()
                                .apply("chinese_name LIKE CONCAT('%', {0}, '%')", predictedName)
                                .or()
                                .apply("{0} LIKE CONCAT('%', chinese_name, '%')", predictedName)
                                .or()
                                .apply("common_name LIKE CONCAT('%', {0}, '%')", predictedName)
                                .or()
                                .apply("{0} LIKE CONCAT('%', common_name, '%')", predictedName)
                                .last("LIMIT 1")
                );
                if (species != null) {
                    log.info("物种模糊匹配成功: {} → {} (speciesId={})", predictedName, species.getChineseName(), species.getId());
                }
            }

            // Tier 3: 向量语义匹配（利用已有知识库，高阈值 0.85 防误匹配）
            if (species == null) {
                try {
                    List<Document> docs = knowledgeBaseService.search(predictedName, 1, 0.85);
                    if (!docs.isEmpty()) {
                        Document topDoc = docs.get(0);
                        Object speciesIdObj = topDoc.getMetadata().get("speciesId");
                        if (speciesIdObj instanceof Long speciesId) {
                            Species vectorMatch = speciesMapper.selectById(speciesId);
                            if (vectorMatch != null) {
                                species = vectorMatch;
                                log.info("物转向量匹配成功: {} → {} (speciesId={})", predictedName, vectorMatch.getChineseName(), speciesId);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("向量匹配异常，跳过: {}", e.getMessage());
                }
            }

            if (species != null) {
                record.setPredictedSpeciesId(species.getId());
                recognitionMapper.updateById(record);

                Map<String, Object> speciesCard = new java.util.HashMap<>();
                speciesCard.put("id", species.getId());
                speciesCard.put("commonName", species.getCommonName());
                speciesCard.put("scientificName", species.getScientificName());
                speciesCard.put("iucnStatus", species.getIucnStatus());
                speciesCard.put("protectionLevel", species.getProtectionLevel());
                speciesCard.put("morphology", species.getMorphology());
                speciesCard.put("ecology", species.getEcology());
                speciesCard.put("description", species.getDescription());

                result = new java.util.HashMap<>();
                result.put("recognition", record);
                result.put("speciesCard", speciesCard);

                if (species.getChineseName() != null && !species.getChineseName().equals(predictedName)) {
                    log.info("物种匹配: {} → speciesId={}", predictedName, species.getId());
                }
            }
        }

        // 7.6 高置信度识别建议创建观测记录（任务 3.5）
        if (record.getConfidenceScore() != null
                && record.getConfidenceScore().compareTo(BigDecimal.valueOf(HIGH_CONFIDENCE_THRESHOLD)) >= 0) {
            if (result == null) {
                result = new java.util.HashMap<>();
                result.put("recognition", record);
            }
            result.put("suggestObservation", true);
            result.put("observationData", Map.of(
                    "speciesId", record.getPredictedSpeciesId() != null ? record.getPredictedSpeciesId() : 0L,
                    "speciesName", record.getPredictedSpeciesName() != null ? record.getPredictedSpeciesName() : "未知",
                    "latitude", latitude != null ? latitude : 0.0,
                    "longitude", longitude != null ? longitude : 0.0,
                    "observationTime", LocalDateTime.now(),
                    "confidence", record.getConfidenceScore()
            ));

            // 7.7 高置信度 + 未匹配物种 → 建议录入新物种
            if (record.getPredictedSpeciesId() == null && parsed != null) {
                result.put("suggestNewSpecies", true);
                result.put("speciesPrefill", buildSpeciesPrefill(parsed));
            }
        }

        log.info("图像识别完成: code={}, species={}, confidence={}, 耗时={}ms",
                record.getRecognitionCode(), record.getPredictedSpeciesName(),
                record.getConfidenceScore(), record.getProcessingTimeMs());

        if (result == null) {
            result = new java.util.HashMap<>();
            result.put("recognition", record);
        }
        return result;
    }

    /**
     * 从 AI 识别结果构建物种新增表单预填数据。
     * 对模型未返回的字段做容错：取不到就跳过，前端留空让用户手动填写。
     */
    private Map<String, Object> buildSpeciesPrefill(JsonNode parsed) {
        Map<String, Object> prefill = new java.util.HashMap<>();

        putIfPresent(prefill, "scientificName", parsed, "scientificName");
        putIfPresent(prefill, "chineseName", parsed, "speciesName");
        putIfPresent(prefill, "commonName", parsed, "commonName");
        putIfPresent(prefill, "description", parsed, "description");
        putIfPresent(prefill, "morphology", parsed, "morphology");
        putIfPresent(prefill, "ecology", parsed, "ecology");
        putIfPresent(prefill, "kingdom", parsed, "kingdom");
        putIfPresent(prefill, "phylum", parsed, "phylum");
        putIfPresent(prefill, "className", parsed, "className");
        putIfPresent(prefill, "orderName", parsed, "orderName");
        putIfPresent(prefill, "family", parsed, "family");
        putIfPresent(prefill, "genus", parsed, "genus");
        putIfPresent(prefill, "species", parsed, "species");
        putIfPresent(prefill, "source", parsed, null); // 固定值

        // IUCN 等级：仅接受标准枚举值，否则跳过
        String iucn = getTextOrNull(parsed, "iucnStatus");
        if (iucn != null && java.util.Set.of("EX", "EW", "CR", "EN", "VU", "NT", "LC").contains(iucn)) {
            prefill.put("iucnStatus", iucn);
        }

        // 保护等级：仅接受 "1"/"2"/"3"，否则跳过
        String protLevel = getTextOrNull(parsed, "protectionLevel");
        if (protLevel != null) {
            // 规范化：AI 可能返回 "国家二级"、"二级"、"2" 等格式
            String normalized = protLevel.replaceAll("[^0-9]", "");
            if (normalized.isEmpty()) {
                // 尝试中文数字映射
                if (protLevel.contains("一")) normalized = "1";
                else if (protLevel.contains("二")) normalized = "2";
                else if (protLevel.contains("三")) normalized = "3";
            }
            if (java.util.Set.of("1", "2", "3").contains(normalized)) {
                prefill.put("protectionLevel", normalized);
            }
        }

        // 布尔标记：默认 0
        prefill.put("isEndemic", parsed.path("isEndemic").asInt(0));
        prefill.put("isInvasive", parsed.path("isInvasive").asInt(0));

        // 固定数据来源标记
        prefill.put("source", "AI 图像识别自动生成");

        return prefill;
    }

    private void putIfPresent(Map<String, Object> target, String targetKey, JsonNode source, String sourceKey) {
        if (sourceKey == null) return; // 固定值由调用方直接设置
        JsonNode node = source.get(sourceKey);
        if (node != null && !node.isNull() && !node.isMissingNode()) {
            String text = node.asText().trim();
            if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                target.put(targetKey, text);
            }
        }
    }

    private String getTextOrNull(JsonNode source, String key) {
        JsonNode node = source.get(key);
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        String text = node.asText().trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    // ==================== 智能问答 SSE 流式（第二层：Prompt模板 + RAG + 多轮对话）====================

    @Override
    public Flux<String> chatStream(ChatDTO dto, AtomicReference<Long> savedIdRef) {
        // 0. 语义缓存检查（任务 3.1）
        String cachedAnswer = semanticCacheService.getCachedAnswer(dto.getQuestion());
        if (cachedAnswer != null) {
            log.info("语义缓存命中，跳过模型调用: {}", dto.getQuestion());
            return streamCachedAnswer(cachedAnswer);
        }

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

        // 2. RAG 检索：从知识库中查找相关文档（任务 2.2）
        final String ragContext = searchRagContext(dto.getQuestion());

        // 3. 获取 Prompt 模板（任务 2.1）
        String baseSystemPrompt = promptTemplateManager.getSystemPrompt(dto.getQuestionType());

        // 4. 将 RAG 上下文注入 System Prompt
        final String finalSystemPrompt;
        if (!ragContext.isEmpty()) {
            finalSystemPrompt = baseSystemPrompt + "\n\n" + ragContext
                    + "\n请基于以上参考资料回答用户问题。如果参考资料不足以回答，可以结合你的知识补充，但需说明哪些是基于资料、哪些是你的推断。";
        } else {
            finalSystemPrompt = baseSystemPrompt;
        }

        // 5. 获取多轮对话历史（任务 2.3）
        List<Message> historyMessages = chatSessionManager.getHistoryMessages(dto.getSessionId());

        // 6. 构造完整消息列表：System + History + Current
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(finalSystemPrompt));
        messages.addAll(historyMessages);
        messages.add(new UserMessage(dto.getQuestion()));

        // 7. 构建流式 Flux —— 不在此处 subscribe，由 Controller 通过 SseEmitter 订阅
        StringBuilder fullAnswer = new StringBuilder();

        return ChatClient.create(chatModel).prompt()
                .options(DashScopeChatOptions.builder()
                        .withModel(aiProperties.getChatModel())
                        .withMaxToken(aiProperties.getMaxTokens())
                        .withTemperature(aiProperties.getTemperature())
                        .build())
                .messages(messages)
                .stream()
                .content()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .doOnNext(fullAnswer::append)
                .doOnComplete(() -> {
                    String answer = fullAnswer.toString();

                    // 保存会话历史到 Redis（任务 2.3）
                    chatSessionManager.saveMessage(
                            dto.getSessionId(), dto.getQuestion(), answer);

                    // 保存完整回答到数据库
                    history.setAnswerText(answer);
                    history.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
                    if (!ragContext.isEmpty()) {
                        history.setSourceReferences("RAG");
                    }
                    try {
                        qaHistoryMapper.insert(history);
                        savedIdRef.set(history.getId());
                        publishChatEvent(history);

                        // 缓存高质量回答（任务 3.1）
                        if (history.getFeedback() == null || history.getFeedback() == 1) {
                            semanticCacheService.cacheAnswer(dto.getQuestion(), answer);
                        }

                        log.info("智能问答完成: code={}, 耗时={}ms, 回答长度={}, RAG={}, 历史轮数={}",
                                history.getQuestionCode(),
                                history.getProcessingTimeMs(),
                                fullAnswer.length(),
                                !ragContext.isEmpty(),
                                historyMessages.size() / 2);
                    } catch (Exception e) {
                        log.error("保存问答记录失败（不影响流式响应）", e);
                    }
                })
                .doOnError(e -> log.error("AI 流式调用异常", e))
                // 流结束后追加一条元数据事件，携带 QaHistory ID 供前端反馈使用
                .concatWith(Mono.fromSupplier(() -> {
                    try {
                        Long qaId = savedIdRef.get();
                        return objectMapper.writeValueAsString(
                                Map.of("qaId", qaId != null ? qaId : 0L));
                    } catch (Exception e) {
                        return "{\"qaId\":0}";
                    }
                }));
    }

    // ==================== 语义缓存辅助方法（任务 3.1）====================

    /**
     * 将缓存答案拆分为 chunk 模拟流式返回
     * <p>
     * 追加元数据事件 qaId=-1 标记为缓存回答，前端可据此展示"来自缓存"标识。
     */
    private Flux<String> streamCachedAnswer(String answer) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < answer.length(); i += STREAM_CHUNK_SIZE) {
            chunks.add(answer.substring(i, Math.min(i + STREAM_CHUNK_SIZE, answer.length())));
        }
        return Flux.fromIterable(chunks)
                .concatWith(Mono.fromCallable(() ->
                        objectMapper.writeValueAsString(Map.of("qaId", -1))));
    }

    // ==================== 历史记录查询 ====================

    @Override
    public Page<ImageRecognition> getRecognitionHistory(Integer page, Integer size) {
        return recognitionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<ImageRecognition>()
                        .orderByDesc(ImageRecognition::getCreateTime)
        );
    }

    @Override
    public ImageRecognition getRecognitionById(Long id) {
        return recognitionMapper.selectById(id);
    }

    @Override
    public Page<QaHistory> getChatHistory(Integer page, Integer size) {
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

    // ==================== 会话管理（任务 2.4）====================

    @Override
    public void clearSession(String sessionId) {
        chatSessionManager.clearSession(sessionId);
        log.info("已清空会话历史: {}", sessionId);
    }

    // ==================== AI 观测记录创建（任务 3.5）====================

    @Override
    public Map<String, Object> createObservationFromAi(AiObservationDTO dto) {
        // 1. 查询识别记录
        ImageRecognition record = recognitionMapper.selectById(dto.getRecognitionId());
        if (record == null) {
            throw new com.oceanverse.common.exception.BusinessException(404, "识别记录不存在");
        }
        if ("识别失败".equals(record.getPredictedSpeciesName())) {
            throw new com.oceanverse.common.exception.BusinessException(400, "该识别记录未成功识别物种，无法创建观测");
        }

        // 2. 查询物种详情（用于填充观测备注）
        String speciesInfo = "";
        if (record.getPredictedSpeciesId() != null) {
            Species species = speciesMapper.selectById(record.getPredictedSpeciesId());
            if (species != null) {
                speciesInfo = String.format("物种：%s（%s），IUCN：%s，保护等级：%s",
                        species.getChineseName() != null ? species.getChineseName() : species.getCommonName(),
                        species.getScientificName(),
                        species.getIucnStatus() != null ? species.getIucnStatus() : "未知",
                        species.getProtectionLevel() != null ? species.getProtectionLevel() : "未知");
            }
        }

        // 3. 构建观测记录
        Observation observation = new Observation();
        observation.setObservationType("SIGHTING");
        observation.setObservationDate(LocalDate.now());
        observation.setObservationTime(LocalTime.now());
        observation.setLatitude(BigDecimal.valueOf(dto.getLatitude()));
        observation.setLongitude(BigDecimal.valueOf(dto.getLongitude()));
        observation.setObserverId(getCurrentUserId());
        observation.setNotes(String.format("【AI 识别生成】识别编号：%s，物种：%s，置信度：%.0f%%。%s",
                record.getRecognitionCode(),
                record.getPredictedSpeciesName(),
                record.getConfidenceScore() != null ? record.getConfidenceScore().doubleValue() * 100 : 0,
                speciesInfo));
        observation.setCreateTime(LocalDateTime.now());
        observation.setUpdateTime(LocalDateTime.now());

        // 4. 自动生成观测编号并插入
        observation.setObservationCode(generateObservationCode());
        observationMapper.insert(observation);

        log.info("从 AI 识别创建观测记录: recognitionCode={}, observationId={}, species={}",
                record.getRecognitionCode(), observation.getId(), record.getPredictedSpeciesName());

        // 5. 返回结果
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("observationId", observation.getId());
        result.put("observationCode", observation.getObservationCode());
        result.put("speciesName", record.getPredictedSpeciesName());
        result.put("confidence", record.getConfidenceScore());
        result.put("latitude", dto.getLatitude());
        result.put("longitude", dto.getLongitude());
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成观测编号（OBS001, OBS002, ...），与 ObservationServiceImpl 保持一致
     */
    private String generateObservationCode() {
        Observation latest = observationMapper.selectOne(
                new LambdaQueryWrapper<Observation>()
                        .orderByDesc(Observation::getId)
                        .last("LIMIT 1"));
        if (latest == null) {
            return "OBS001";
        }
        String code = latest.getObservationCode();
        if (code == null || !code.startsWith("OBS")) {
            return "OBS001";
        }
        try {
            int num = Integer.parseInt(code.substring(3));
            return String.format("OBS%03d", num + 1);
        } catch (NumberFormatException e) {
            return "OBS001";
        }
    }

    /**
     * 从知识库中检索 RAG 上下文
     * <p>
     * 检索失败时返回空字符串，降级为裸模型回答。
     */
    private String searchRagContext(String question) {
        try {
            List<Document> relevantDocs = knowledgeBaseService.search(question, RAG_TOP_K);
            String context = knowledgeBaseService.formatContext(relevantDocs);
            log.info("RAG 检索完成: 命中 {} 个文档, 上下文长度={}", relevantDocs.size(), context.length());
            return context;
        } catch (Exception e) {
            log.warn("RAG 检索异常，降级为裸模型: {}", e.getMessage());
            return "";
        }
    }

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
                    "recognitionId", record.getId() != null ? record.getId() : 0L,
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
