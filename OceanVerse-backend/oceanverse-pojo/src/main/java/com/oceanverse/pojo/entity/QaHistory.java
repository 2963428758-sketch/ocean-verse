package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 智能问答历史
 */
@Data
@TableName("qa_history")
public class QaHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String questionCode;
    private String questionType;
    private String questionText;
    private String questionCategory;
    private Long userId;
    private String sessionId;
    private String aiModelVersion;
    private String answerText;
    private String sourceReferences;
    private java.math.BigDecimal confidenceScore;
    private Integer answerTokens;
    private Integer processingTimeMs;
    private Integer feedback;
    private String feedbackText;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
