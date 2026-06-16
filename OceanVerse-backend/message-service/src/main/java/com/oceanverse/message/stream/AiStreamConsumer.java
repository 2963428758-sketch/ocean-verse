package com.oceanverse.message.stream;

import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.message.config.RedisStreamConfig;
import com.oceanverse.message.dto.AiEvent;
import com.oceanverse.message.dto.NotificationMessage;
import com.oceanverse.message.service.NotificationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * AI 事件消费者 — 监听 stream:ai
 * <p>
 * 接收 AI 模块发布的事件（图像识别完成、问答会话、用户反馈等），
 * 对需要通知用户的事件构建 NotificationMessage 并转发到 stream:notification。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiStreamConsumer extends StreamConsumer {

    private final RedisStreamConfig streamConfig;
    private final NotificationService notificationService;

    @PostConstruct
    public void init() {
        register(
                streamConfig,
                CommonConstants.STREAM_AI,
                CommonConstants.STREAM_CONSUMER_GROUP,
                CommonConstants.STREAM_CONSUMER_NAME + "-ai"
        );
        log.info("AI 事件消费者已启动");
    }

    @Override
    protected void onMessage(Map<String, String> fields, String streamKey) {
        String type = fields.get(CommonConstants.FIELD_TYPE);
        String payload = fields.get(CommonConstants.FIELD_PAYLOAD);

        if (payload == null || payload.isBlank()) {
            log.warn("收到空 AI 事件，跳过: type={}", type);
            return;
        }

        try {
            AiEvent event = parsePayload(payload, AiEvent.class);
            if (event == null) {
                log.error("AI 事件反序列化失败: payload={}", payload);
                return;
            }

            log.info("收到 AI 事件: action={}, userId={}", event.getAction(), event.getUserId());

            switch (event.getAction()) {
                case CommonConstants.ACTION_RECOGNITION_COMPLETE:
                    handleRecognitionComplete(event);
                    break;
                case CommonConstants.ACTION_CHAT_SESSION:
                    log.info("问答会话结束: userId={}, questionType={}",
                            event.getUserId(), event.getQuestionType());
                    break;
                case CommonConstants.ACTION_FEEDBACK:
                    log.info("用户反馈: userId={}", event.getUserId());
                    break;
                default:
                    log.warn("未知 AI 事件类型: action={}", event.getAction());
            }

        } catch (Exception e) {
            log.error("处理 AI 事件异常: type={}, payload={}", type, payload, e);
        }
    }

    /**
     * 处理识别完成事件 — 通知用户识别结果
     */
    private void handleRecognitionComplete(AiEvent event) {
        if (event.getUserId() == null) {
            log.warn("识别完成事件缺少用户 ID，跳过通知");
            return;
        }

        // 格式化置信度为百分比
        String confidenceStr = "未知";
        if (event.getConfidence() != null) {
            BigDecimal pct = BigDecimal.valueOf(event.getConfidence() * 100)
                    .setScale(1, RoundingMode.HALF_UP);
            confidenceStr = pct + "%";
        }

        String speciesName = event.getSpeciesName() != null ? event.getSpeciesName() : "待确认";

        NotificationMessage msg = NotificationMessage.builder()
                .userId(event.getUserId())
                .notificationType(CommonConstants.NOTIFY_AI_RESULT)
                .title("图像识别已完成")
                .content("识别结果：" + speciesName + "（置信度 " + confidenceStr + "）")
                .relatedId(null)
                .build();

        notificationService.publishNotification(msg);
        log.info("识别完成通知已转发: userId={}, species={}", event.getUserId(), speciesName);
    }
}
