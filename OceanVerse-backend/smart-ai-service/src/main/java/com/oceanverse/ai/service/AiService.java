package com.oceanverse.ai.service;

import com.oceanverse.pojo.dto.ChatDTO;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

public interface AiService {
    /**
     * 图像识别
     */
    Object recognizeImage(MultipartFile file, Double latitude, Double longitude);

    /**
     * 智能问答（SSE 流式）— 返回 Flux<String>，由 Controller 通过 SseEmitter 推送
     */
    Flux<String> chatStream(ChatDTO dto);

    /**
     * 识别历史
     */
    Object getRecognitionHistory(Integer page, Integer size);

    /**
     * 问答历史
     */
    Object getChatHistory(Integer page, Integer size);

    /**
     * 问答反馈
     */
    void feedback(Long id, Integer feedback);

    /**
     * 清空对话会话历史（任务 2.4）
     */
    void clearSession(String sessionId);
}
