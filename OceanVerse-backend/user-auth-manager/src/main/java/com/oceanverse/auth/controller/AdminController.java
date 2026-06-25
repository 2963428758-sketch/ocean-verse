package com.oceanverse.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.auth.mapper.LoginLogMapper;
import com.oceanverse.auth.mapper.OperateLogMapper;
import com.oceanverse.common.annotation.OperateLog;
import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.annotation.RequireRole;
import com.oceanverse.auth.service.UserService;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.result.PageResult;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.AssignRolesDTO;
import com.oceanverse.pojo.dto.UpdateStatusDTO;
import com.oceanverse.pojo.entity.SysLoginLog;
import com.oceanverse.pojo.entity.SysOperateLog;
import com.oceanverse.pojo.vo.UserListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员后台 — 用户管理 + 日志查询接口
 */
@Tag(name = "Admin", description = "管理员后台 — 用户管理、日志查询")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole({"SUPER_ADMIN", "ADMIN"})
public class AdminController {

    private final UserService userService;
    private final LoginLogMapper loginLogMapper;
    private final OperateLogMapper operateLogMapper;

    // ==================== 用户管理 ====================

    @Operation(summary = "用户分页列表", description = "按关键字搜索用户，支持分页")
    @GetMapping("/user/list")
    @RequirePermission("user:list")
    public Result<PageResult<UserListVO>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        size = Math.min(size, CommonConstants.MAX_PAGE_SIZE);
        return Result.success(userService.listUsers(page, size, keyword));
    }

    @OperateLog(module = "用户管理", type = OperateLog.OperateType.UPDATE)
    @Operation(summary = "修改用户状态", description = "启用/禁用/锁定用户，禁用后 Token 和权限缓存被清除")
    @PutMapping("/user/{userId}/status")
    @RequirePermission("user:status")
    public Result<Void> updateStatus(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Valid @RequestBody UpdateStatusDTO dto) {
        userService.updateUserStatus(userId, dto.getStatus());
        return Result.success();
    }

    @OperateLog(module = "用户管理", type = OperateLog.OperateType.UPDATE)
    @Operation(summary = "分配用户角色", description = "为用户分配角色（全量替换），先删后插")
    @PostMapping("/user/{userId}/roles")
    @RequirePermission("user:assign-role")
    public Result<Void> assignRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Valid @RequestBody AssignRolesDTO dto) {
        dto.setUserId(userId);
        userService.assignRoles(dto);
        return Result.success();
    }

    @OperateLog(module = "用户管理", type = OperateLog.OperateType.OTHER)
    @Operation(summary = "强制下线", description = "清除用户缓存 + Token 黑名单，立即踢出当前会话")
    @PostMapping("/user/{userId}/force-logout")
    @RequirePermission("user:force-logout")
    public Result<Void> forceLogout(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        userService.forceLogout(userId);
        return Result.success();
    }

    // ==================== 日志查询 ====================

    @Operation(summary = "登录日志分页列表", description = "按用户名、登录状态筛选登录日志")
    @GetMapping("/log/login/list")
    @RequirePermission("log:login")
    public Result<PageResult<SysLoginLog>> loginLogList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "登录状态：1=成功, 0=失败") @RequestParam(required = false) Integer status) {
        size = Math.min(size, CommonConstants.MAX_PAGE_SIZE);
        Page<SysLoginLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.eq(SysLoginLog::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(SysLoginLog::getStatus, status);
        }
        wrapper.orderByDesc(SysLoginLog::getLoginTime);
        Page<SysLoginLog> result = loginLogMapper.selectPage(pageParam, wrapper);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "操作日志分页列表", description = "按模块、操作人筛选操作日志")
    @GetMapping("/log/operation/list")
    @RequirePermission("log:operate")
    public Result<PageResult<SysOperateLog>> operationLogList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "模块名") @RequestParam(required = false) String module,
            @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        size = Math.min(size, CommonConstants.MAX_PAGE_SIZE);
        Page<SysOperateLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysOperateLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.eq(SysOperateLog::getModule, module);
        }
        if (StringUtils.hasText(operatorName)) {
            wrapper.eq(SysOperateLog::getOperatorName, operatorName);
        }
        wrapper.orderByDesc(SysOperateLog::getCreateTime);
        Page<SysOperateLog> result = operateLogMapper.selectPage(pageParam, wrapper);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }
}
