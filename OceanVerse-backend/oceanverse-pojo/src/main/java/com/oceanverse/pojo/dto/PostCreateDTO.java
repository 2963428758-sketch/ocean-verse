package com.oceanverse.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostCreateDTO {
    @NotBlank(message = "内容不能为空")
    private String content;
    /** 类型: NORMAL, OBSERVATION, RECOGNITION */
    private String postType;
    private Long relatedSpeciesId;
    private Long relatedObservationId;
    /** 图片URL列表 */
    private String imageUrls;
}
