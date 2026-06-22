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
    @Schema(description = "验证码唯一标识，提交登录/注册时需回传此 key")
    private String captchaKey;
    @Schema(description = "验证码图片，Base64 编码的 PNG 格式，前缀 data:image/png;base64,")
    private String imageBase64;
    @Schema(description = "验证码过期时间（秒）")
    private Long expireSeconds;
}
