package com.oceanverse.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 事件 DTO — 用于 stream:ai
 * <p>
 * AI 模块在图像识别完成、问答会话结束时发布此事件，
 * 由 AiStreamConsumer 消费并决定是否需要通知用户。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEvent {

    /** 事件动作: RECOGNITION_COMPLETE / CHAT_SESSION / FEEDBACK */
    private String action;

    /** 操作用户 ID */
    private Long userId;

    /** 识别记录编码（识别完成时） */
    private String recognitionCode;

    /** 识别记录数据库 ID（用于通知关联跳转） */
    private Long recognitionId;

    /** 识别结果物种名（识别完成时） */
    private String speciesName;

    /** 置信度 0.0~1.0（识别完成时） */
    private Double confidence;

    /** 问答类型（聊天时） */
    private String questionType;
}
