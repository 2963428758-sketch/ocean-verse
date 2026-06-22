package com.oceanverse.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50")
    @Schema(description = "用户名（3-50字符）", example = "newuser")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度6-50")
    @Schema(description = "密码（6-50字符）")
    private String password;
    @NotBlank(message = "验证码标识不能为空")
    @Schema(description = "验证码 Key（调用 /api/captcha 获取）")
    private String captchaKey;
    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码答案")
    private String captchaCode;
}
