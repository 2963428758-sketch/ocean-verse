package com.oceanverse.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表 VO（管理后台分页列表）
 */
@Data
public class UserListVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String realName;
    private String avatarUrl;

    /** 角色标识 */
    private String role;

    /** 账号状态: 0-禁用, 1-正常, 2-锁定 */
    private Integer status;

    /** 数据权限范围 */
    private Integer dataScope;

    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
