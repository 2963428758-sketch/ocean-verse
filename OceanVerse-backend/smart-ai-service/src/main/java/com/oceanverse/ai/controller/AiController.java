package com.oceanverse.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.ai.eval.AiEvalService;
import com.oceanverse.ai.ratelimit.AiRateLimiter;
import com.oceanverse.common.result.Result;
import com.oceanverse.common.utils.JwtUtil;
import com.oceanverse.pojo.dto.AiObservationDTO;
import com.oceanverse.pojo.dto.ChatDTO;
import com.oceanverse.pojo.entity.ImageRecognition;
import com.oceanverse.pojo.entity.QaHistory;
import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.ai.rag.KnowledgeBaseService;
import com.oceanverse.ai.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 智能服务接口 — 成员B/E
 * 包含：图像识别、智能问答、数据分析
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;
    private final AiRateLimiter aiRateLimiter;
    private final AiEvalService aiEvalService;

    /**
     * 图像识别 — 上传照片识别海洋生物
     */
    @PostMapping("/recognize")
    public Result<Map<String, Object>> recognize(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "latitude", required = false) Double latitude,
                                     @RequestParam(value = "longitude", required = false) Double longitude) {
        // 限流检查（任务 3.2）
        Long userId = getCurrentUserId();
        if (!aiRateLimiter.isAllowed(userId, "recognition")) {
            return Result.fail("今日识别次数已用完，请明天再试");
        }
        aiRateLimiter.recordCall(userId, "recognition");
        return Result.success(aiService.recognizeImage(file, latitude, longitude));
    }

    /**
     * 智能问答 — SSE 流式返回
     * <p>
     * 使用 SseEmitter 管理 SSE 连接生命周期，避免 HttpServletResponse 被提前回收。
     * 每个 chunk 通过 JSON 编码发送，确保内容中的 \n 被转义为 \\n，
     * 避免被 SSE 协议当作事件分隔符吃掉。前端用 JSON.parse 还原原始文本。
     * <p>
     * SseEmitter 超时/完成时取消 Flux 订阅，避免模型继续生成无用内容。
     */
    @PostMapping("/chat")
    public SseEmitter chat(@Valid @RequestBody ChatDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L);

        // 限流检查（任务 3.2）
        Long userId = getCurrentUserId();
        if (!aiRateLimiter.isAllowed(userId, "chat")) {
            try {
                String errorMsg = objectMapper.writeValueAsString("今日问答次数已用完，请明天再试");
                emitter.send(SseEmitter.event().data(errorMsg));
                emitter.send(SseEmitter.event().data("[ERROR]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        aiRateLimiter.recordCall(userId, "chat");

        AtomicReference<Long> savedIdRef = new AtomicReference<>();

        Disposable subscription = aiService.chatStream(dto, savedIdRef)
                .subscribe(
                        chunk -> {
                            try {
                                // JSON 编码：\n → \\n，" → \"，保证数据行内无真实换行
                                String json = objectMapper.writeValueAsString(chunk);
                                emitter.send(SseEmitter.event().data(json));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event().data("[ERROR]"));
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

        // 客户端断开或超时时取消 Flux 订阅，停止模型生成
        emitter.onTimeout(() -> {
            subscription.dispose();
        });
        emitter.onCompletion(() -> {
            subscription.dispose();
        });

        return emitter;
    }

    /**
     * 获取识别历史
     */
    @GetMapping("/recognition/history")
    public Result<Page<ImageRecognition>> getRecognitionHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(aiService.getRecognitionHistory(page, size));
    }

    /**
     * 获取单条识别记录详情（通知点击跳转用）
     */
    @GetMapping("/recognition/{id}")
    public Result<ImageRecognition> getRecognitionById(@PathVariable Long id) {
        ImageRecognition record = aiService.getRecognitionById(id);
        if (record == null) {
            return Result.error("识别记录不存在");
        }
        return Result.success(record);
    }

    /**
     * 获取问答历史
     */
    @GetMapping("/chat/history")
    public Result<Page<QaHistory>> getChatHistory(
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

    /**
     * 清空对话会话历史（任务 2.4）
     * <p>
     * 删除 Redis 中指定 sessionId 的对话历史，
     * 前端"新对话"按钮调用此接口后，AI 不再引用之前的对话内容。
     */
    @DeleteMapping("/chat/session/{sessionId}")
    public Result<Void> clearSession(@PathVariable String sessionId) {
        aiService.clearSession(sessionId);
        return Result.success();
    }

    /**
     * 从 AI 识别结果创建观测记录（任务 3.5）
     * <p>
     * 前端点击"记录观测"后调用，基于识别记录自动创建观测数据，
     * 关联 eco-observation-manager 模块。
     */
    @PostMapping("/observation")
    public Result<Map<String, Object>> createObservationFromAi(@Valid @RequestBody AiObservationDTO dto) {
        return Result.success(aiService.createObservationFromAi(dto));
    }

    /**
     * 重建知识库索引（异步）
     * <p>
     * 从 Authorization 头解析管理员 userId，触发后台异步重建。
     * 接口立即返回，重建完成后通过通知系统告知结果。
     */
    @RequirePermission("ai:knowledge:rebuild")
    @PostMapping("/knowledge/rebuild")
    public Result<String> rebuildKnowledgeBase(@RequestHeader(value = "Authorization", required = false) String token) {
        if (knowledgeBaseService.isRebuilding()) {
            return Result.error("知识库正在重建中，请稍后再试");
        }

        Long userId = parseUserIdFromToken(token);
        knowledgeBaseService.rebuildIndexAsync(userId);
        return Result.success("知识库重建已启动，完成后将通过通知告知结果");
    }

    /**
     * 查询剩余调用配额（任务 3.2）
     */
    @GetMapping("/quota")
    public Result<Object> getQuota() {
        Long userId = getCurrentUserId();
        Map<String, Object> quota = new HashMap<>();
        quota.put("chatRemaining", aiRateLimiter.getRemainingQuota(userId, "chat"));
        quota.put("recognitionRemaining", aiRateLimiter.getRemainingQuota(userId, "recognition"));
        return Result.success(quota);
    }

    /**
     * AI 效果评估统计（任务 3.3）
     */
    @GetMapping("/eval/stats")
    public Result<Object> getEvalStats() {
        return Result.success(aiEvalService.getEvalStats());
    }

    // ==================== 辅助方法 ====================

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

    /**
     * 从 Authorization 头解析用户 ID
     * <p>
     * 用于未被 JwtInterceptor 拦截的路径（如 /api/ai/**），
     * 直接从 JWT token 中提取 userId。
     */
    private Long parseUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String rawToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            return JwtUtil.getUserId(rawToken);
        } catch (Exception e) {
            log.warn("从 Authorization 头解析 userId 失败: {}", e.getMessage());
            return null;
        }
    }
}
