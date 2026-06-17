package com.oceanverse.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表 VO（管理后台分页列表）
 */
@Data
@Schema(description = "用户列表项")
public class UserListVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "角色标识")
    private String role;

    @Schema(description = "账号状态: 0-禁用, 1-正常, 2-锁定")
    private Integer status;

    @Schema(description = "数据权限范围")
    private Integer dataScope;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
