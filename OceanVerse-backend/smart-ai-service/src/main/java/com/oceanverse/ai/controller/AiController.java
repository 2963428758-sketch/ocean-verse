package com.oceanverse.ai.controller;

import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.ChatDTO;
import com.oceanverse.ai.service.AiService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 智能服务接口 — 成员B/E
 * 包含：图像识别、智能问答、数据分析
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * 图像识别 — 上传照片识别海洋生物
     */
    @PostMapping("/recognize")
    public Result<Object> recognize(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "latitude", required = false) Double latitude,
                                     @RequestParam(value = "longitude", required = false) Double longitude) {
        return Result.success(aiService.recognizeImage(file, latitude, longitude));
    }

    /**
     * 智能问答 — SSE 流式返回
     */
    @PostMapping("/chat")
    public void chat(@RequestBody ChatDTO dto, HttpServletResponse response) {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        aiService.chatStream(dto, response);
    }

    /**
     * 获取识别历史
     */
    @GetMapping("/recognition/history")
    public Result<Object> getRecognitionHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(aiService.getRecognitionHistory(page, size));
    }

    /**
     * 获取问答历史
     */
    @GetMapping("/chat/history")
    public Result<Object> getChatHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(aiService.getChatHistory(page, size));
    }

    /**
     * 问答反馈
     */
    @PostMapping("/chat/feedback/{id}")
    public Result<Void> feedback(@PathVariable Long id, @RequestParam Integer feedback) {
        aiService.feedback(id, feedback);
        return Result.success();
    }
}
