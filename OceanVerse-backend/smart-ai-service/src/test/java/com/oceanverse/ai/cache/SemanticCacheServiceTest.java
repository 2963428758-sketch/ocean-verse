package com.oceanverse.ai.cache;

import com.oceanverse.ai.config.AiProperties;
import com.oceanverse.ai.mapper.QaHistoryMapper;
import com.oceanverse.pojo.entity.QaHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SemanticCacheService 单元测试（双层缓存升级版）
 * <p>
 * 覆盖场景：
 * - Layer 1 MD5 精确匹配（Redis 快速通道）
 * - Layer 2 语义向量匹配（Embedding 余弦相似度）
 * - Layer 1 回填（Layer 2 命中后自动写入 Redis）
 * - 缓存预热（双层加载 + 磁盘持久化）
 * - 异常容错（Redis/向量存储故障不影响主流程）
 */
@ExtendWith(MockitoExtension.class)
class SemanticCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private QaHistoryMapper qaHistoryMapper;

    @Mock
    private EmbeddingModel embeddingModel;

    private AiProperties aiProperties;
    private SemanticCacheService cacheService;

    @BeforeEach
    void setUp() {
        // EmbeddingModel mock: 返回零向量（SimpleVectorStore 会正常存储但相似度为 0）
        lenient().when(embeddingModel.embed(any(org.springframework.ai.document.Document.class)))
                .thenReturn(new float[0]);

        aiProperties = new AiProperties();
        // cacheVectorStorePath 保持默认 smart-ai-service/data/ai/cache-vector-store.json（磁盘文件不存在，load 会跳过）
        // cacheSemanticThreshold 默认 0.90
        // cacheTtlHours 默认 24L

        cacheService = new SemanticCacheService(redisTemplate, qaHistoryMapper,
                embeddingModel, aiProperties);
    }

    // ==================== Layer 1: MD5 精确匹配 ====================

    @Test
    void getCachedAnswer_layer1ExactHit_returnsAnswer() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("绿海龟是濒危物种。");

        String result = cacheService.getCachedAnswer("绿海龟的保护现状");

        assertEquals("绿海龟是濒危物种。", result);
    }

    @Test
    void getCachedAnswer_sameQuestion_sameRedisKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        cacheService.getCachedAnswer("绿海龟");
        cacheService.getCachedAnswer("绿海龟");

        // 同一个问题两次 MD5 应该生成相同的 key
        verify(valueOperations, times(2)).get(anyString());
    }

    // ==================== Layer 2: 语义向量匹配 ====================

    @Test
    void getCachedAnswer_layer2NoHitBelowThreshold_returnsNull() {
        // Layer 1 miss
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Layer 2: 零向量 → 余弦相似度 = 0 < 阈值 0.90 → 无匹配
        String result = cacheService.getCachedAnswer("蓝鲸有多大");

        assertNull(result);
    }

    // ==================== 双层 miss ====================

    @Test
    void getCachedAnswer_bothLayersMiss_returnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        String result = cacheService.getCachedAnswer("蓝鲸有多大");

        assertNull(result);
    }

    // ==================== 异常容错 ====================

    @Test
    void getCachedAnswer_redisException_returnsNull() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        String result = cacheService.getCachedAnswer("任何问题");

        assertNull(result);
    }

    @Test
    void cacheAnswer_redisException_swallowed() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        assertDoesNotThrow(() -> cacheService.cacheAnswer("问题", "答案"));
    }

    // ==================== 缓存写入（双层） ====================

    @Test
    void cacheAnswer_storesToRedisWithTTL() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.cacheAnswer("绿海龟吃什么？", "绿海龟主要以海草为食。");

        verify(valueOperations).set(
                anyString(),
                eq("绿海龟主要以海草为食。"),
                eq(24L),
                eq(TimeUnit.HOURS)
        );
    }

    @Test
    void cacheAnswer_invokesEmbeddingModel() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.cacheAnswer("绿海龟吃什么？", "绿海龟主要以海草为食。");

        // 验证 EmbeddingModel 被调用（SimpleVectorStore.add 内部触发）
        verify(embeddingModel, atLeastOnce()).embed(any(org.springframework.ai.document.Document.class));
    }

    @Test
    void cacheAnswer_respectsConfiguredTTL() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        aiProperties.setCacheTtlHours(48);

        cacheService.cacheAnswer("珊瑚礁白化？", "水温升高导致共生藻离开。");

        verify(valueOperations).set(anyString(), anyString(), eq(48L), eq(TimeUnit.HOURS));
    }

    // ==================== 缓存预热 ====================

    @Test
    void warmupCache_loadsHighQualityAnswersToBothLayers() {
        QaHistory qa1 = new QaHistory();
        qa1.setQuestionText("绿海龟的保护等级？");
        qa1.setAnswerText("绿海龟是 IUCN 濒危物种。");
        qa1.setFeedback(1);

        QaHistory qa2 = new QaHistory();
        qa2.setQuestionText("珊瑚礁白化原因？");
        qa2.setAnswerText("海水温度升高导致共生藻离开。");
        qa2.setFeedback(1);

        when(qaHistoryMapper.selectList(any())).thenReturn(List.of(qa1, qa2));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.warmupCache();

        // 2 条数据各写入 Redis 一次 = 2 次
        verify(valueOperations, times(2)).set(anyString(), anyString(), eq(24L), eq(TimeUnit.HOURS));
        // 2 条数据各触发一次 EmbeddingModel 调用 = 2 次
        verify(embeddingModel, times(2)).embed(any(org.springframework.ai.document.Document.class));
    }

    @Test
    void warmupCache_emptyTable_loadsZero() {
        when(qaHistoryMapper.selectList(any())).thenReturn(List.of());

        cacheService.warmupCache();

        verifyNoInteractions(redisTemplate);
        verifyNoInteractions(embeddingModel);
    }

    @Test
    void warmupCache_mapperException_swallowed() {
        when(qaHistoryMapper.selectList(any())).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> cacheService.warmupCache());
    }

    // ==================== 生命周期 ====================

    @Test
    void saveOnShutdown_doesNotThrow() {
        assertDoesNotThrow(() -> cacheService.saveOnShutdown());
    }
}
