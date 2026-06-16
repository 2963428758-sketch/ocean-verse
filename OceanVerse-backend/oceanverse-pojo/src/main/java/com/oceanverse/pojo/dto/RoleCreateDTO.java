package com.oceanverse.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色创建 DTO
 */
@Data
@Schema(description = "创建角色请求")
public class RoleCreateDTO {

    @NotBlank(message = "角色代码不能为空")
    @Size(max = 50, message = "角色代码长度不能超过50")
    @Schema(description = "角色代码（唯一标识）", example = "RESEARCHER")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称长度不能超过100")
    @Schema(description = "角色名称", example = "研究员")
    private String roleName;

    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "角色描述")
    private String description;
}
