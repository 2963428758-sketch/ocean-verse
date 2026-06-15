package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 图像识别记录
 */
@Data
@TableName("image_recognition")
public class ImageRecognition {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recognitionCode;
    private String imageUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private String aiModelVersion;
    private String recognitionResult;
    private Long predictedSpeciesId;
    private String predictedSpeciesName;
    private java.math.BigDecimal confidenceScore;
    private String topPredictions;
    private String recognitionType;
    private String verificationStatus;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;
    private Long observationId;
    private Long userId;
    private Integer processingTimeMs;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
