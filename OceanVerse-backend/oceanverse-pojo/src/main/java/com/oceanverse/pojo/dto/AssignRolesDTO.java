package com.oceanverse.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户分配角色 DTO
 */
@Data
@Schema(description = "用户分配角色请求")
public class AssignRolesDTO {

    @Schema(description = "用户ID（由路径参数自动填充）")
    private Long userId;

    @NotNull(message = "角色列表不能为空")
    @Schema(description = "角色ID列表（全量替换，空列表表示清除全部角色）")
    private List<Long> roleIds;
}
