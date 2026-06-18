package com.oceanverse.ai.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oceanverse.ai.config.AiProperties;
import com.oceanverse.ai.mapper.QaHistoryMapper;
import com.oceanverse.pojo.entity.QaHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 语义缓存服务（任务 3.1 — 向量语义匹配升级版）
 * <p>
 * 双层缓存策略，同时支持精确匹配和语义相似度匹配：
 * <ul>
 *   <li><b>Layer 1 — MD5 精确匹配</b>：O(1) Redis 查找，同字符同序的同一问题命中快速通道。</li>
 *   <li><b>Layer 2 — Embedding 语义匹配</b>：将问题转为向量，在 {@link SimpleVectorStore} 中
 *       做余弦相似度检索。相似度 ≥ 阈值（默认 0.90）即视为缓存命中，可覆盖
 *       "绿海龟吃什么" ≈ "绿海龟的食物是什么" 这类语义等价问题。</li>
 * </ul>
 * <p>
 * 缓存写入时同时更新 Redis 和向量存储；语义命中后自动回填 Redis（Layer 1 backfill），
 * 后续相同问题走快速通道，避免重复 Embedding 调用。
 * <p>
 * 预热：应用启动后从数据库加载高满意度问答到双层存储。
 */
@Slf4j
@Service
public class SemanticCacheService {

    private static final String CACHE_KEY_PREFIX = "ai:cache:";

    /** 向量存储中答案文本的 metadata key */
    private static final String ANSWER_META_KEY = "cached_answer";

    private final StringRedisTemplate redisTemplate;
    private final QaHistoryMapper qaHistoryMapper;
    private final EmbeddingModel embeddingModel;
    private final AiProperties aiProperties;

    /** 缓存专用向量存储（独立于 RAG 知识库的 VectorStore Bean） */
    private final AtomicReference<SimpleVectorStore> cacheStore = new AtomicReference<>();

    public SemanticCacheService(StringRedisTemplate redisTemplate,
                                QaHistoryMapper qaHistoryMapper,
                                EmbeddingModel embeddingModel,
                                AiProperties aiProperties) {
        this.redisTemplate = redisTemplate;
        this.qaHistoryMapper = qaHistoryMapper;
        this.embeddingModel = embeddingModel;
        this.aiProperties = aiProperties;
        this.cacheStore.set(SimpleVectorStore.builder(embeddingModel).build());
    }

    // ==================== 缓存查询 ====================

    /**
     * 尝试从缓存中获取答案（双层检索）
     * <p>
     * 查找顺序：
     * <ol>
     *   <li>Layer 1 — MD5 精确匹配（Redis GET，O(1)）</li>
     *   <li>Layer 2 — 语义向量匹配（Embedding + 余弦相似度，topK=1）</li>
     * </ol>
     * Layer 2 命中后自动回填 Layer 1 的 Redis Key，后续同一问题走快速通道。
     *
     * @return 缓存的答案，未命中返回 null
     */
    public String getCachedAnswer(String question) {
        try {
            // === Layer 1: MD5 精确匹配 ===
            String cacheKey = CACHE_KEY_PREFIX + md5(question);
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("语义缓存命中 [Layer1-精确]: {}", question);
                return cached;
            }

            // === Layer 2: 语义向量匹配 ===
            return searchBySemantic(question, cacheKey);

        } catch (Exception e) {
            log.warn("语义缓存查询失败: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 缓存写入 ====================

    /**
     * 将高质量问答同时存入双层缓存
     * <p>
     * Layer 1: Redis（MD5 Key，TTL 可配置）<br>
     * Layer 2: 向量存储（Question 向量化，Answer 存入 metadata）
     */
    public void cacheAnswer(String question, String answer) {
        try {
            // Layer 1: Redis
            String cacheKey = CACHE_KEY_PREFIX + md5(question);
            redisTemplate.opsForValue().set(cacheKey, answer,
                    aiProperties.getCacheTtlHours(), TimeUnit.HOURS);

            // Layer 2: 向量存储
            addToCacheVectorStore(question, answer);

            log.info("已缓存问答（双层）: {}", question);
        } catch (Exception e) {
            log.warn("语义缓存写入失败: {}", e.getMessage());
        }
    }

    // ==================== 缓存预热 ====================

    /**
     * 缓存预热：应用启动完成后从数据库加载高满意度问答到双层存储
     * <p>
     * 使用 {@link ApplicationReadyEvent} 而非 @PostConstruct，
     * 确保所有 Bean（含 MyBatis Mapper、EmbeddingModel）初始化完毕后再执行。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {
        try {
            // 尝试从磁盘加载向量索引
            loadCacheVectorStoreFromDisk();

            // 加载高质量问答到双层缓存
            List<QaHistory> highQualityAnswers = qaHistoryMapper.selectList(
                    new LambdaQueryWrapper<QaHistory>()
                            .eq(QaHistory::getFeedback, 1)
                            .isNotNull(QaHistory::getAnswerText)
                            .last("LIMIT 100")
            );

            int redisCount = 0;
            int vectorCount = 0;
            for (QaHistory history : highQualityAnswers) {
                // Layer 1: Redis
                String cacheKey = CACHE_KEY_PREFIX + md5(history.getQuestionText());
                redisTemplate.opsForValue().set(cacheKey, history.getAnswerText(),
                        aiProperties.getCacheTtlHours(), TimeUnit.HOURS);
                redisCount++;

                // Layer 2: 向量存储
                addToCacheVectorStore(history.getQuestionText(), history.getAnswerText());
                vectorCount++;
            }

            // 持久化向量索引到磁盘
            saveCacheVectorStoreToDisk();

            log.info("缓存预热完成: Redis {} 条, 向量索引 {} 条（共加载 {} 条高质量问答）",
                    redisCount, vectorCount, highQualityAnswers.size());
        } catch (Exception e) {
            log.warn("缓存预热失败（不影响正常运行）: {}", e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    /**
     * Layer 2 语义检索：将问题向量化后在缓存向量存储中查找最相似的历史问题
     * <p>
     * 命中条件：top-1 结果的相似度 ≥ 配置阈值（默认 0.82）。
     * 命中后自动回填 Layer 1 Redis Key，实现"一次 Embedding，永久快速通道"。
     *
     * @param question  用户问题
     * @param redisKey  Layer 1 的 Redis Key（用于回填）
     * @return 缓存的答案，未达阈值返回 null
     */
    private String searchBySemantic(String question, String redisKey) {
        SimpleVectorStore store = cacheStore.get();
        double threshold = aiProperties.getCacheSemanticThreshold();

        // 先做诊断搜索（无阈值过滤），获取实际相似度分数用于调参参考
        List<Document> candidates = store.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .similarityThreshold(0.0)
                        .build()
        );

        if (candidates.isEmpty()) {
            log.debug("语义缓存向量存储为空，跳过 Layer2 检索");
            return null;
        }

        // 记录候选结果的实际分数（便于观察和调参）
        for (int i = 0; i < candidates.size(); i++) {
            Document doc = candidates.get(i);
            String cachedQ = doc.getText().length() > 40
                    ? doc.getText().substring(0, 40) + "..."
                    : doc.getText();
            log.info("语义缓存候选[{}]: score={}, question=\"{}\"",
                    i + 1, doc.getScore(), cachedQ);
        }

        // 检查 top-1 是否达到阈值
        Document best = candidates.get(0);
        if (best.getScore() != null && best.getScore() >= threshold) {
            String answer = best.getMetadata().get(ANSWER_META_KEY).toString();

            // 回填 Layer 1：后续相同问题直接走 MD5 快速通道
            try {
                redisTemplate.opsForValue().set(redisKey, answer,
                        aiProperties.getCacheTtlHours(), TimeUnit.HOURS);
            } catch (Exception e) {
                log.debug("Layer1 回填失败（不影响本次返回）: {}", e.getMessage());
            }

            log.info("语义缓存命中 [Layer2-语义]: {} (score={}, 阈值={})",
                    question, best.getScore(), threshold);
            return answer;
        }

        log.info("语义缓存未命中 [Layer2]: 最高相似度 {} < 阈值 {}",
                best.getScore(), threshold);
        return null;
    }

    /**
     * 向缓存向量存储中添加一条问答
     * <p>
     * 使用 UUID 作为文档 ID 避免相同问题重复写入时 ID 冲突。
     */
    private void addToCacheVectorStore(String question, String answer) {
        try {
            Document doc = new Document(
                    java.util.UUID.randomUUID().toString(),
                    question,
                    Map.of(ANSWER_META_KEY, answer)
            );
            cacheStore.get().add(List.of(doc));
        } catch (Exception e) {
            log.debug("向量存储添加缓存条目失败: {}", e.getMessage());
        }
    }

    /**
     * 从磁盘加载缓存向量索引（避免重复 Embedding 调用）
     */
    private void loadCacheVectorStoreFromDisk() {
        String path = aiProperties.getCacheVectorStorePath();
        if (path != null && !path.isEmpty()) {
            File storeFile = new File(path);
            if (storeFile.exists()) {
                cacheStore.get().load(storeFile);
                log.info("已从磁盘加载缓存向量索引: {}", path);
            }
        }
    }

    /**
     * 将缓存向量索引持久化到磁盘
     */
    private void saveCacheVectorStoreToDisk() {
        String path = aiProperties.getCacheVectorStorePath();
        if (path != null && !path.isEmpty()) {
            try {
                File storeFile = new File(path);
                File parentDir = storeFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                cacheStore.get().save(storeFile);
                log.info("缓存向量索引已持久化: {}", path);
            } catch (Exception e) {
                log.warn("缓存向量索引持久化失败（不影响功能）: {}", e.getMessage());
            }
        }
    }

    /**
     * 应用关闭前持久化缓存向量索引
     */
    @PreDestroy
    public void saveOnShutdown() {
        saveCacheVectorStoreToDisk();
        log.info("缓存向量索引已在关闭前持久化");
    }

    private String md5(String input) {
        return org.springframework.util.DigestUtils.md5DigestAsHex(input.getBytes());
    }
}
