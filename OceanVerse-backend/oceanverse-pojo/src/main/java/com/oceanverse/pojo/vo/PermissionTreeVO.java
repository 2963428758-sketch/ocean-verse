package com.oceanverse.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 权限树 VO（支持递归嵌套）
 */
@Data
@Schema(description = "权限树节点")
public class PermissionTreeVO {

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "父ID")
    private Long parentId;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限标识")
    private String permCode;

    @Schema(description = "类型: MENU / BUTTON / API")
    private String type;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "子权限列表")
    private List<PermissionTreeVO> children;
}
