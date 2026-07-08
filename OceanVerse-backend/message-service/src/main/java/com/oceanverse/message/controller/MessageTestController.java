package com.oceanverse.message.controller;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.result.Result;
import com.oceanverse.message.dto.*;
import com.oceanverse.message.service.RedisStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列测试接口 — 仅用于开发和测试环境
 * <p>
 * 提供向各个 Stream 发布测试消息的接口，
 * 用于验证消费者链路是否正常工作。
 * <p>
 * 通过 @Profile 限制仅在 dev/test 环境生效，生产环境不会注册此 Controller。
 */
@Slf4j
@RestController
@RequestMapping("/api/message/test")
@RequiredArgsConstructor
@Profile({"dev", "test"})
public class MessageTestController {

    private final RedisStreamService redisStreamService;

    // ==================== stream:notification ====================

    /**
     * 发送测试通知消息
     * <p>
     * 预期效果：
     * 1. 控制台输出 "收到通知消息" 日志
     * 2. sys_notification 表新增一条记录
     * 3. 如果目标用户 WebSocket 在线，会收到实时推送
     */
    @PostMapping("/notification")
    public Result<Map<String, Object>> testNotification(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "LIKE") String type) {

        NotificationMessage msg = NotificationMessage.builder()
                .userId(userId)
                .notificationType(type)
                .title("【测试】" + type + " 通知")
                .content("这是一条测试通知消息，来自 MessageTestController")
                .senderId(999L)
                .senderName("test-sender")
                .relatedId(1L)
                .build();

        RecordId recordId = redisStreamService.publish(
                CommonConstants.STREAM_NOTIFICATION, msg);

        return Result.success(buildResult(recordId, CommonConstants.STREAM_NOTIFICATION, type));
    }

    // ==================== stream:community ====================

    /**
     * 发送测试社区事件 — 模拟评论
     * <p>
     * 预期效果：
     * 1. CommunityStreamConsumer 收到事件
     * 2. 如果 actorId != targetUserId，会转发一条通知到 stream:notification
     * 3. NotificationStreamConsumer 处理通知 → 入库 + WebSocket 推送
     */
    @PostMapping("/community/comment")
    public Result<Map<String, Object>> testCommunityComment(
            @RequestParam(defaultValue = "1") Long actorId,
            @RequestParam(defaultValue = "2") Long targetUserId,
            @RequestParam(defaultValue = "1") Long postId) {

        CommunityEvent event = CommunityEvent.builder()
                .action(CommonConstants.ACTION_COMMENT_CREATED)
                .actorId(actorId)
                .actorName("test-actor")
                .targetUserId(targetUserId)
                .targetId(postId)
                .targetType("POST")
                .content("这是一条测试评论内容，用于验证社区事件消费链路")
                .build();

        RecordId recordId = redisStreamService.publish(
                CommonConstants.STREAM_COMMUNITY, event);

        return Result.success(buildResult(recordId, CommonConstants.STREAM_COMMUNITY, "COMMENT_CREATED"));
    }

    /**
     * 发送测试社区事件 — 模拟点赞
     * <p>
     * 预期效果同上，通知标题变为"有人点赞了你的动态"
     */
    @PostMapping("/community/like")
    public Result<Map<String, Object>> testCommunityLike(
            @RequestParam(defaultValue = "1") Long actorId,
            @RequestParam(defaultValue = "2") Long targetUserId,
            @RequestParam(defaultValue = "1") Long targetId,
            @RequestParam(defaultValue = "POST") String targetType) {

        CommunityEvent event = CommunityEvent.builder()
                .action(CommonConstants.ACTION_LIKE_TOGGLED)
                .actorId(actorId)
                .actorName("test-actor")
                .targetUserId(targetUserId)
                .targetId(targetId)
                .targetType(targetType)
                .build();

        RecordId recordId = redisStreamService.publish(
                CommonConstants.STREAM_COMMUNITY, event);

        return Result.success(buildResult(recordId, CommonConstants.STREAM_COMMUNITY, "LIKE_TOGGLED"));
    }

    // ==================== stream:ai ====================

    /**
     * 发送测试 AI 事件 — 模拟图像识别完成
     * <p>
     * 预期效果：
     * 1. AiStreamConsumer 收到事件
     * 2. 构建"图像识别已完成"通知并转发到 stream:notification
     * 3. NotificationStreamConsumer 处理 → 入库 + WebSocket 推送
     */
    @PostMapping("/ai/recognition")
    public Result<Map<String, Object>> testAiRecognition(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "蓝鳍金枪鱼") String speciesName,
            @RequestParam(defaultValue = "0.92") Double confidence) {

        AiEvent event = AiEvent.builder()
                .action(CommonConstants.ACTION_RECOGNITION_COMPLETE)
                .userId(userId)
                .recognitionCode("REC-TEST-001")
                .speciesName(speciesName)
                .confidence(confidence)
                .build();

        RecordId recordId = redisStreamService.publish(
                CommonConstants.STREAM_AI, event);

        return Result.success(buildResult(recordId, CommonConstants.STREAM_AI, "RECOGNITION_COMPLETE"));
    }

    // ==================== stream:system ====================

    /**
     * 发送测试系统事件 — 模拟用户注册
     * <p>
     * 预期效果：
     * 1. SystemStreamConsumer 收到事件
     * 2. 控制台输出 "新用户注册" 日志
     * 3. Redis 中 stats:total_users 计数器 +1
     */
    @PostMapping("/system")
    public Result<Map<String, Object>> testSystem(
            @RequestParam(defaultValue = "USER_REGISTER") String action,
            @RequestParam(defaultValue = "user:100") String targetKey,
            @RequestParam(defaultValue = "test-new-user") String data) {

        SystemEvent event = SystemEvent.builder()
                .action(action)
                .targetKey(targetKey)
                .data(data)
                .build();

        RecordId recordId = redisStreamService.publish(
                CommonConstants.STREAM_SYSTEM, event);

        return Result.success(buildResult(recordId, CommonConstants.STREAM_SYSTEM, action));
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> buildResult(RecordId recordId, String stream, String type) {
        Map<String, Object> result = new HashMap<>();
        result.put("streamKey", stream);
        result.put("messageId", recordId != null ? recordId.getValue() : "null");
        result.put("eventType", type);
        result.put("hint", "请查看后端控制台日志确认消费者是否正常处理");
        return result;
    }
}
