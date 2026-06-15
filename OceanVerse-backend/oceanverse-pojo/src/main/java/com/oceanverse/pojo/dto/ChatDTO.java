package com.oceanverse.pojo.dto;

import lombok.Data;

@Data
public class ChatDTO {
    private String question;
    private String sessionId;
    private String questionType;
}
