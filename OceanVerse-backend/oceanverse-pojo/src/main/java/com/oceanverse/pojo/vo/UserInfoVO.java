package com.oceanverse.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 VO（详情）
 */
@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String realName;
    private String avatarUrl;

    /** 角色标识 */
    private String role;

    /** 权限码列表 */
    private List<String> permissions;

    /** 数据权限范围: 1-仅本人, 2-全部 */
    private Integer dataScope;

    /** 账号状态: 0-禁用, 1-正常, 2-锁定 */
    private Integer status;

    private LocalDateTime createTime;
}
