package com.oceanverse.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改个人信息请求")
public class UpdateProfileDTO {
    @Size(max = 30, message = "昵称长度不超过30")
    @Schema(description = "昵称")
    private String nickname;
    @Size(max = 50, message = "真实姓名长度不超过50")
    @Schema(description = "真实姓名")
    private String realName;
    @Schema(description = "头像URL")
    private String avatarUrl;
}
