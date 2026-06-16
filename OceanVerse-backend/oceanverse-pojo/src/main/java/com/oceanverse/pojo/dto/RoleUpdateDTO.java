package com.oceanverse.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色更新 DTO
 */
@Data
@Schema(description = "更新角色请求")
public class RoleUpdateDTO {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称长度不能超过100")
    @Schema(description = "角色名称")
    private String roleName;

    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;
}
