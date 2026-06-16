package com.oceanverse.pojo.vo;

import lombok.Data;

/**
 * 登录成功响应 VO
 */
@Data
public class LoginVO {

    /** AccessToken（2h 有效） */
    private String accessToken;

    /** RefreshToken（7d 有效） */
    private String refreshToken;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 角色标识 */
    private String role;

    /** 头像URL */
    private String avatarUrl;
}
