package com.oceanverse.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改个人信息请求")
public class UpdateProfileDTO {
    @Size(max = 50, message = "真实姓名长度不超过50")
    @Schema(description = "真实姓名")
    private String realName;
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址")
    private String email;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "头像URL")
    private String avatarUrl;
}
