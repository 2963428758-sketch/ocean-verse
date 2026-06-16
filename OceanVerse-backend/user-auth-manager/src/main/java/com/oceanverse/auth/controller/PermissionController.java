package com.oceanverse.auth.controller;

import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.annotation.RequireRole;
import com.oceanverse.auth.context.UserContext;
import com.oceanverse.auth.service.PermissionService;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.vo.PermissionTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理接口
 * <p>
 * 权限树查询接口需登录；管理类查询需要 SUPER_ADMIN 或 ADMIN 角色。
 * Swagger Tag: Permission（权限管理）
 *
 * @author OceanVerse
 */
@Tag(name = "Permission", description = "权限管理接口")
@RestController
@RequestMapping("/api/admin/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "获取全量权限树", description = "返回嵌套的权限树结构，用于前端权限分配页面")
    @GetMapping("/tree")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("permission:list")
    public Result<List<PermissionTreeVO>> getPermissionTree() {
        return Result.success(permissionService.getPermissionTree());
    }

    @Operation(summary = "获取当前用户权限码", description = "返回当前登录用户拥有的所有权限码列表")
    @GetMapping("/my")
    public Result<List<String>> getMyPermissions() {
        Long userId = UserContext.getUserId();
        return Result.success(permissionService.getUserPermCodes(userId));
    }

    @Operation(summary = "获取用户权限码", description = "根据用户ID获取权限码列表（管理用）")
    @GetMapping("/user/{userId}")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("permission:list")
    public Result<List<String>> getUserPermissions(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        return Result.success(permissionService.getUserPermCodes(userId));
    }

    @Operation(summary = "获取角色权限ID列表", description = "查询指定角色关联的权限ID列表")
    @GetMapping("/role/{roleId}")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("permission:list")
    public Result<List<Long>> getRolePermissions(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        return Result.success(permissionService.getRolePermIds(roleId));
    }
}
