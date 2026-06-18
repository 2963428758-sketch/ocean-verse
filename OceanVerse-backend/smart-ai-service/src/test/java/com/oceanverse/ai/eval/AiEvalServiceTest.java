package com.oceanverse.ai.eval;

import com.oceanverse.ai.mapper.ImageRecognitionMapper;
import com.oceanverse.ai.mapper.QaHistoryMapper;
import com.oceanverse.pojo.entity.ImageRecognition;
import com.oceanverse.pojo.entity.QaHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AiEvalService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AiEvalServiceTest {

    @Mock
    private QaHistoryMapper qaHistoryMapper;

    @Mock
    private ImageRecognitionMapper recognitionMapper;

    private AiEvalService evalService;

    @BeforeEach
    void setUp() {
        evalService = new AiEvalService(qaHistoryMapper, recognitionMapper);
    }

    @Test
    void getEvalStats_emptyDatabase_returnsZeros() {
        when(qaHistoryMapper.selectList(null)).thenReturn(List.of());
        when(recognitionMapper.selectList(null)).thenReturn(List.of());

        Map<String, Object> stats = evalService.getEvalStats();

        assertEquals(0L, stats.get("qaTotal"));
        assertEquals(0L, stats.get("qaSatisfied"));
        assertEquals(BigDecimal.ZERO, stats.get("qaSatisfactionRate"));
        assertEquals(0L, stats.get("recognitionTotal"));
        assertEquals(BigDecimal.ZERO, stats.get("recognitionAccuracyRate"));
    }

    @Test
    void getEvalStats_satisfactionRate_calculatesCorrectly() {
        QaHistory satisfied = new QaHistory();
        satisfied.setFeedback(1);
        satisfied.setProcessingTimeMs(1000);

        QaHistory unsatisfied = new QaHistory();
        unsatisfied.setFeedback(0);
        unsatisfied.setProcessingTimeMs(2000);

        QaHistory noFeedback = new QaHistory();
        noFeedback.setProcessingTimeMs(500);

        when(qaHistoryMapper.selectList(null)).thenReturn(List.of(satisfied, unsatisfied, noFeedback));
        when(recognitionMapper.selectList(null)).thenReturn(List.of());

        Map<String, Object> stats = evalService.getEvalStats();

        assertEquals(3L, stats.get("qaTotal"));
        assertEquals(1L, stats.get("qaSatisfied"));
        assertEquals(1L, stats.get("qaUnsatisfied"));
        // 满意率 = 1/3 ≈ 33.3%
        assertEquals(new BigDecimal("33.3"), stats.get("qaSatisfactionRate"));
        // 平均耗时 = (1000+2000+500)/3 = 1166
        assertEquals(1166, stats.get("qaAvgProcessingTimeMs"));
    }

    @Test
    void getEvalStats_topQuestions_sortedByFrequency() {
        QaHistory q1 = new QaHistory();
        q1.setQuestionText("绿海龟");
        q1.setProcessingTimeMs(100);
        QaHistory q2 = new QaHistory();
        q2.setQuestionText("绿海龟");
        q2.setProcessingTimeMs(100);
        QaHistory q3 = new QaHistory();
        q3.setQuestionText("蓝鲸");
        q3.setProcessingTimeMs(100);

        when(qaHistoryMapper.selectList(null)).thenReturn(List.of(q1, q2, q3));
        when(recognitionMapper.selectList(null)).thenReturn(List.of());

        Map<String, Object> stats = evalService.getEvalStats();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topQuestions = (List<Map<String, Object>>) stats.get("qaTopQuestions");
        assertNotNull(topQuestions);
        assertEquals(2, topQuestions.size());
        assertEquals("绿海龟", topQuestions.get(0).get("question"));
        assertEquals(2L, topQuestions.get(0).get("count"));
    }

    @Test
    void getEvalStats_typeDistribution_groupsByType() {
        QaHistory species1 = new QaHistory();
        species1.setQuestionType("SPECIES");
        species1.setProcessingTimeMs(100);
        QaHistory species2 = new QaHistory();
        species2.setQuestionType("SPECIES");
        species2.setProcessingTimeMs(100);
        QaHistory general = new QaHistory();
        general.setQuestionType("GENERAL");
        general.setProcessingTimeMs(100);

        when(qaHistoryMapper.selectList(null)).thenReturn(List.of(species1, species2, general));
        when(recognitionMapper.selectList(null)).thenReturn(List.of());

        Map<String, Object> stats = evalService.getEvalStats();

        @SuppressWarnings("unchecked")
        Map<String, Long> distribution = (Map<String, Long>) stats.get("qaTypeDistribution");
        assertNotNull(distribution);
        assertEquals(2L, distribution.get("SPECIES"));
        assertEquals(1L, distribution.get("GENERAL"));
    }

    @Test
    void getEvalStats_recognitionAccuracy_calculatesCorrectly() {
        ImageRecognition verified = new ImageRecognition();
        verified.setVerificationStatus("VERIFIED");
        verified.setConfidenceScore(new BigDecimal("0.92"));
        verified.setProcessingTimeMs(3000);

        ImageRecognition pending = new ImageRecognition();
        pending.setVerificationStatus("PENDING");
        pending.setConfidenceScore(new BigDecimal("0.65"));
        pending.setProcessingTimeMs(5000);

        when(qaHistoryMapper.selectList(null)).thenReturn(List.of());
        when(recognitionMapper.selectList(null)).thenReturn(List.of(verified, pending));

        Map<String, Object> stats = evalService.getEvalStats();

        assertEquals(2L, stats.get("recognitionTotal"));
        assertEquals(1L, stats.get("recognitionVerified"));
        // 准确率 = 1/2 = 50.0%
        assertEquals(new BigDecimal("50.0"), stats.get("recognitionAccuracyRate"));
        // 平均识别耗时 = (3000+5000)/2 = 4000
        assertEquals(4000, stats.get("recognitionAvgProcessingTimeMs"));
    }

    @Test
    void getEvalStats_highConfidenceRate_calculatesCorrectly() {
        ImageRecognition high1 = new ImageRecognition();
        high1.setConfidenceScore(new BigDecimal("0.85"));
        high1.setProcessingTimeMs(1000);
        ImageRecognition high2 = new ImageRecognition();
        high2.setConfidenceScore(new BigDecimal("0.90"));
        high2.setProcessingTimeMs(1000);
        ImageRecognition low = new ImageRecognition();
        low.setConfidenceScore(new BigDecimal("0.40"));
        low.setProcessingTimeMs(1000);

        when(qaHistoryMapper.selectList(null)).thenReturn(List.of());
        when(recognitionMapper.selectList(null)).thenReturn(List.of(high1, high2, low));

        Map<String, Object> stats = evalService.getEvalStats();

        // 高置信度占比 = 2/3 ≈ 66.7%
        assertEquals(new BigDecimal("66.7"), stats.get("recognitionHighConfidenceRate"));
    }
}
