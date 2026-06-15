package com.oceanverse.ai.service;

import com.oceanverse.pojo.dto.ChatDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AiService {
    /**
     * 图像识别
     */
    Object recognizeImage(MultipartFile file, Double latitude, Double longitude);

    /**
     * 智能问答（SSE 流式）
     */
    void chatStream(ChatDTO dto, HttpServletResponse response);

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
}
