package com.oceanverse.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码响应")
public class CaptchaVO {
    @Schema(description = "验证码唯一标识，提交登录/注册时需回传此 key", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String captchaKey;
    @Schema(description = "验证码算式（文本形式），例如 3 + 5 = ?", example = "3 + 5 = ?")
    private String expression;
    @Schema(description = "验证码过期时间（秒）", example = "300")
    private Long expireSeconds;
}
