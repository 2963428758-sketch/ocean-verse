package com.oceanverse.ai.controller;

import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.ChatDTO;
import com.oceanverse.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
     * <p>
     * 使用 SseEmitter 管理 SSE 连接生命周期，避免 HttpServletResponse 被提前回收。
     * Service 层返回 Flux<String>，Controller 负责订阅并将事件推送到客户端。
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody ChatDTO dto) {
        // 超时设为 120 秒，覆盖长对话场景
        SseEmitter emitter = new SseEmitter(120_000L);

        aiService.chatStream(dto)
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event().data(chunk));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event().data("[ERROR] AI 服务暂时不可用，请稍后重试"));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().data("[DONE]"));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }
                );

        return emitter;
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
     * @param feedback 1=满意，0=不满意
     */
    @PostMapping("/chat/feedback/{id}")
    public Result<Void> feedback(@PathVariable Long id, @RequestParam Integer feedback) {
        if (feedback == null || (feedback != 0 && feedback != 1)) {
            return Result.error("反馈参数无效，仅支持 0（不满意）或 1（满意）");
        }
        aiService.feedback(id, feedback);
        return Result.success();
    }
}
