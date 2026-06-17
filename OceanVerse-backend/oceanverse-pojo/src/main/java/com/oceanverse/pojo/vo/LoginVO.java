package com.oceanverse.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录成功响应 VO
 */
@Data
@Schema(description = "登录成功响应")
public class LoginVO {

    @Schema(description = "AccessToken（2h 有效）")
    private String accessToken;

    @Schema(description = "RefreshToken（7d 有效）")
    private String refreshToken;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色标识")
    private String role;

    @Schema(description = "头像URL")
    private String avatarUrl;
}
