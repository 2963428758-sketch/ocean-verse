package com.oceanverse.auth.controller;

import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.annotation.RequireRole;
import com.oceanverse.auth.service.UserService;
import com.oceanverse.common.result.PageResult;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.AssignRolesDTO;
import com.oceanverse.pojo.vo.UserListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员后台 — 用户管理接口
 * <p>
 * 所有接口需要 SUPER_ADMIN 或 ADMIN 角色（通过 @RequireRole 注解控制）。
 * 移除了原手动 isManager() 判断，全面改用声明式权限注解。
 * <p>
 * Swagger Tag: Admin（管理员后台）
 *
 * @author OceanVerse
 */
@Tag(name = "Admin", description = "管理员后台 — 用户管理接口")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
@RequireRole({"SUPER_ADMIN", "ADMIN"})
public class AdminController {

    private final UserService userService;

    @Operation(summary = "用户分页列表", description = "按关键字搜索用户，支持分页")
    @GetMapping("/list")
    @RequirePermission("user:list")
    public Result<PageResult<UserListVO>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        return Result.success(userService.listUsers(page, size, keyword));
    }

    @Operation(summary = "修改用户状态", description = "启用/禁用/锁定用户，禁用后 Token 和权限缓存被清除")
    @PutMapping("/{userId}/status")
    @RequirePermission("user:update")
    public Result<Void> updateStatus(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            return Result.fail("status 不能为空");
        }
        userService.updateUserStatus(userId, status);
        return Result.success();
    }

    @Operation(summary = "分配用户角色", description = "为用户分配角色（全量替换），先删后插")
    @PostMapping("/{userId}/roles")
    @RequirePermission("user:assign-role")
    public Result<Void> assignRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Valid @RequestBody AssignRolesDTO dto) {
        dto.setUserId(userId);
        userService.assignRoles(dto);
        return Result.success();
    }

    @Operation(summary = "强制下线", description = "清除用户缓存 + Token 黑名单，立即踢出当前会话")
    @PostMapping("/{userId}/force-logout")
    @RequirePermission("user:force-logout")
    public Result<Void> forceLogout(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        userService.forceLogout(userId);
        return Result.success();
    }
}
