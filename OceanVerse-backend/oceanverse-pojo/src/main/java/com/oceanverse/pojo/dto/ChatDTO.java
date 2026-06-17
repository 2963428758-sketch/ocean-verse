package com.oceanverse.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatDTO {
    @NotBlank(message = "问题内容不能为空")
    private String question;
    private String sessionId;
    private String questionType;
}
