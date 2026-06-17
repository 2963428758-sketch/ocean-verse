package com.oceanverse.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色 VO
 */
@Data
@Schema(description = "角色信息")
public class RoleVO {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色代码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态: 0-禁用, 1-正常")
    private Integer status;

    @Schema(description = "关联权限ID列表")
    private List<Long> permissionIds;

    @Schema(description = "关联权限名称列表（前端展示用）")
    private List<String> permissionNames;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
