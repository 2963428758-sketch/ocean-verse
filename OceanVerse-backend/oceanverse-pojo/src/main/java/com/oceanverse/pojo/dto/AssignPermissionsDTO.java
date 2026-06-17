package com.oceanverse.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色分配权限 DTO
 */
@Data
@Schema(description = "角色分配权限请求")
public class AssignPermissionsDTO {

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID")
    private Long roleId;

    @NotNull(message = "权限列表不能为空")
    @Schema(description = "权限ID列表（全量替换，空列表表示清除全部权限）")
    private List<Long> permissionIds;
}
