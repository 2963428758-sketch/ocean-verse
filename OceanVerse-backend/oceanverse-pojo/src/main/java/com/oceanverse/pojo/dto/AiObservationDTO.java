package com.oceanverse.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 识别结果转观测记录 DTO（任务 3.5）
 * <p>
 * 前端在高置信度识别后点击"记录观测"时提交，
 * 后端根据 recognitionId 查询识别记录，自动填充物种、坐标等信息创建观测记录。
 */
@Data
public class AiObservationDTO {

    /**
     * 图像识别记录 ID（从 image_recognition 表中获取物种、置信度等）
     */
    @NotNull(message = "识别记录 ID 不能为空")
    private Long recognitionId;

    /**
     * 观测纬度
     */
    @NotNull(message = "纬度不能为空")
    private Double latitude;

    /**
     * 观测经度
     */
    @NotNull(message = "经度不能为空")
    private Double longitude;
}
