package com.oceanverse.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.ai.mapper.ImageRecognitionMapper;
import com.oceanverse.ai.mapper.QaHistoryMapper;
import com.oceanverse.ai.service.AiService;
import com.oceanverse.pojo.dto.ChatDTO;
import com.oceanverse.pojo.entity.ImageRecognition;
import com.oceanverse.pojo.entity.QaHistory;
import com.oceanverse.common.utils.OssUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ImageRecognitionMapper recognitionMapper;
    private final QaHistoryMapper qaHistoryMapper;
    private final OssUtil ossUtil;

    @Override
    public Object recognizeImage(MultipartFile file, Double latitude, Double longitude) {
        // TODO: 接入真实 AI 识别 API（通义千问 qwen-vl-max）
        // 当前返回模拟数据，文件已上传至 OSS
        String imageUrl = ossUtil.upload(file);

        ImageRecognition record = new ImageRecognition();
        record.setRecognitionCode("REC" + UUID.randomUUID().toString().substring(0, 8));
        record.setImageUrl(imageUrl);
        record.setFileName(file.getOriginalFilename());
        record.setFileSize(file.getSize());
        record.setAiModelVersion("mock-v1.0");
        record.setPredictedSpeciesName("待识别");
        record.setConfidenceScore(new java.math.BigDecimal("0.00"));
        record.setRecognitionType("AUTO");
        record.setVerificationStatus("PENDING");
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        recognitionMapper.insert(record);

        log.info("图像已上传至 OSS 并创建识别记录: {}", record.getRecognitionCode());
        return record;
    }

    @Override
    public void chatStream(ChatDTO dto, HttpServletResponse response) {
        // TODO: 接入真实 AI 大模型 API（DeepSeek / 通义千问）
        // 当前返回模拟流式数据
        try {
            PrintWriter writer = response.getWriter();

            // 保存问答记录
            QaHistory history = new QaHistory();
            history.setQuestionCode("QA" + UUID.randomUUID().toString().substring(0, 8));
            history.setQuestionType(dto.getQuestionType() != null ? dto.getQuestionType() : "GENERAL");
            history.setQuestionText(dto.getQuestion());
            history.setSessionId(dto.getSessionId());
            history.setAiModelVersion("mock-v1.0");
            history.setCreateTime(LocalDateTime.now());
            history.setUpdateTime(LocalDateTime.now());
            history.setStatus(1);

            // 模拟流式回复
            String mockAnswer = "感谢您的提问！这是一个关于「" + dto.getQuestion() + "」的好问题。" +
                    "目前 AI 服务为演示模式，正式版将接入 DeepSeek / 通义千问大模型，" +
                    "提供专业的海洋生物知识问答服务。";
            history.setAnswerText(mockAnswer);

            String[] words = mockAnswer.split("");
            StringBuilder buffer = new StringBuilder();
            for (String word : words) {
                buffer.append(word);
                writer.write("data: " + word + "\n\n");
                writer.flush();
                Thread.sleep(50);
            }
            writer.write("data: [DONE]\n\n");
            writer.flush();

            qaHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("SSE 流式响应异常: ", e);
        }
    }

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

    @Override
    public void feedback(Long id, Integer feedback) {
        QaHistory history = qaHistoryMapper.selectById(id);
        if (history != null) {
            history.setFeedback(feedback);
            history.setFeedbackText(feedback == 1 ? "满意" : "不满意");
            history.setUpdateTime(LocalDateTime.now());
            qaHistoryMapper.updateById(history);
        }
    }
}
