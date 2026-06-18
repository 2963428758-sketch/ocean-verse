package com.oceanverse.ai.eval;

import com.oceanverse.ai.mapper.ImageRecognitionMapper;
import com.oceanverse.ai.mapper.QaHistoryMapper;
import com.oceanverse.pojo.entity.ImageRecognition;
import com.oceanverse.pojo.entity.QaHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 效果评估服务（任务 3.3）
 * <p>
 * 从 qa_history 和 image_recognition 表中聚合统计数据，
 * 提供满意度、响应时间、热门问题、识别准确率等指标，
 * 支撑数据驱动的 AI 模块迭代优化。
 */
@Service
@RequiredArgsConstructor
public class AiEvalService {

    private final QaHistoryMapper qaHistoryMapper;
    private final ImageRecognitionMapper recognitionMapper;

    /**
     * 获取 AI 效果评估统计
     */
    public Map<String, Object> getEvalStats() {
        Map<String, Object> stats = new HashMap<>();

        // ===== 问答统计 =====
        List<QaHistory> allQa = qaHistoryMapper.selectList(null);
        long totalQa = allQa.size();
        long satisfiedQa = allQa.stream()
                .filter(q -> q.getFeedback() != null && q.getFeedback() == 1)
                .count();
        long unsatisfiedQa = allQa.stream()
                .filter(q -> q.getFeedback() != null && q.getFeedback() == 0)
                .count();

        stats.put("qaTotal", totalQa);
        stats.put("qaSatisfied", satisfiedQa);
        stats.put("qaUnsatisfied", unsatisfiedQa);
        stats.put("qaSatisfactionRate", totalQa > 0
                ? BigDecimal.valueOf(satisfiedQa * 100.0 / totalQa).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // 平均响应时间
        double avgProcessingTime = allQa.stream()
                .filter(q -> q.getProcessingTimeMs() != null)
                .mapToInt(QaHistory::getProcessingTimeMs)
                .average()
                .orElse(0);
        stats.put("qaAvgProcessingTimeMs", (int) avgProcessingTime);

        // 热门问题 Top 10（按提问次数）
        Map<String, Long> questionFrequency = allQa.stream()
                .filter(q -> q.getQuestionText() != null)
                .collect(Collectors.groupingBy(
                        QaHistory::getQuestionText,
                        Collectors.counting()
                ));

        List<Map<String, Object>> topQuestions = questionFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("question", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .toList();
        stats.put("qaTopQuestions", topQuestions);

        // 问题类型分布
        Map<String, Long> typeDistribution = allQa.stream()
                .filter(q -> q.getQuestionType() != null)
                .collect(Collectors.groupingBy(
                        QaHistory::getQuestionType,
                        Collectors.counting()
                ));
        stats.put("qaTypeDistribution", typeDistribution);

        // ===== 识别统计 =====
        List<ImageRecognition> allRecognitions = recognitionMapper.selectList(null);
        long totalRecognitions = allRecognitions.size();
        long verifiedRecognitions = allRecognitions.stream()
                .filter(r -> "VERIFIED".equals(r.getVerificationStatus()))
                .count();

        stats.put("recognitionTotal", totalRecognitions);
        stats.put("recognitionVerified", verifiedRecognitions);
        stats.put("recognitionAccuracyRate", totalRecognitions > 0
                ? BigDecimal.valueOf(verifiedRecognitions * 100.0 / totalRecognitions)
                        .setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // 平均识别耗时
        double avgRecognitionTime = allRecognitions.stream()
                .filter(r -> r.getProcessingTimeMs() != null)
                .mapToInt(ImageRecognition::getProcessingTimeMs)
                .average()
                .orElse(0);
        stats.put("recognitionAvgProcessingTimeMs", (int) avgRecognitionTime);

        // 高置信度识别占比
        long highConfidence = allRecognitions.stream()
                .filter(r -> r.getConfidenceScore() != null
                        && r.getConfidenceScore().compareTo(new BigDecimal("0.8")) >= 0)
                .count();
        stats.put("recognitionHighConfidenceRate", totalRecognitions > 0
                ? BigDecimal.valueOf(highConfidence * 100.0 / totalRecognitions)
                        .setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        return stats;
    }
}
