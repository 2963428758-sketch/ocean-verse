package com.oceanverse.auth.controller;

import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.annotation.RequireRole;
import com.oceanverse.auth.service.RoleService;
import com.oceanverse.common.result.PageResult;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.AssignPermissionsDTO;
import com.oceanverse.pojo.dto.RoleCreateDTO;
import com.oceanverse.pojo.dto.RoleUpdateDTO;
import com.oceanverse.pojo.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 角色管理接口
 * <p>
 * 所有接口需要 SUPER_ADMIN 或 ADMIN 角色。
 * Swagger Tag: Role（角色管理）
 *
 * @author OceanVerse
 */
@Tag(name = "Role", description = "角色管理接口")
@RestController
@RequestMapping("/api/admin/role")
@RequiredArgsConstructor
@RequireRole({"SUPER_ADMIN", "ADMIN"})
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色分页列表", description = "按关键字搜索角色，支持分页")
    @GetMapping("/list")
    @RequirePermission("role:list")
    public Result<PageResult<RoleVO>> listRoles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        return Result.success(roleService.listRoles(page, size, keyword));
    }

    @Operation(summary = "角色详情", description = "根据角色ID获取详情，含关联权限")
    @GetMapping("/{roleId}")
    @RequirePermission("role:list")
    public Result<RoleVO> getRole(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        return Result.success(roleService.getRole(roleId));
    }

    @Operation(summary = "创建角色", description = "创建新角色，角色代码不可重复")
    @PostMapping
    @RequirePermission("role:create")
    public Result<Void> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        roleService.createRole(dto);
        return Result.success();
    }

    @Operation(summary = "更新角色", description = "更新角色名称、描述、状态")
    @PutMapping("/{roleId}")
    @RequirePermission("role:update")
    public Result<Void> updateRole(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @Valid @RequestBody RoleUpdateDTO dto) {
        roleService.updateRole(roleId, dto);
        return Result.success();
    }

    @Operation(summary = "删除角色", description = "逻辑删除角色，有用户关联时无法删除")
    @DeleteMapping("/{roleId}")
    @RequirePermission("role:delete")
    public Result<Void> deleteRole(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return Result.success();
    }

    @Operation(summary = "启用/禁用角色", description = "切换角色状态，禁用后关联用户权限缓存被清除")
    @PutMapping("/{roleId}/status")
    @RequirePermission("role:update")
    public Result<Void> toggleStatus(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            return Result.fail("status 不能为空");
        }
        roleService.toggleStatus(roleId, status);
        return Result.success();
    }

    @Operation(summary = "分配角色权限", description = "为角色分配权限（全量替换），先删后插")
    @PostMapping("/assign-permissions")
    @RequirePermission("role:assign-perm")
    public Result<Void> assignPermissions(@Valid @RequestBody AssignPermissionsDTO dto) {
        roleService.assignPermissions(dto);
        return Result.success();
    }
}
