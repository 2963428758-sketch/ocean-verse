package com.oceanverse.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 VO（详情）
 */
@Data
@Schema(description = "用户详情信息")
public class UserInfoVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "角色标识")
    private String role;

    @Schema(description = "权限码列表")
    private List<String> permissions;

    @Schema(description = "数据权限范围: 1-仅本人, 2-全部")
    private Integer dataScope;

    @Schema(description = "账号状态: 0-禁用, 1-正常, 2-锁定")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
