package com.oceanverse.auth.controller;

import com.oceanverse.auth.context.UserContext;
import com.oceanverse.auth.service.UserService;
import com.oceanverse.common.result.PageResult;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.vo.UserListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/list")
    public Result<PageResult<UserListVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        if (!isManager()) {
            return Result.error(403, "没有权限");
        }
        return Result.success(userService.listUsers(page, size, keyword));
    }

    @PutMapping("/{userId}/status")
    public Result<Void> updateStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Integer> body) {
        if (!isManager()) {
            return Result.error(403, "没有权限");
        }
        Integer status = body.get("status");
        if (status == null) {
            return Result.fail("status 不能为空");
        }
        userService.updateUserStatus(userId, status);
        return Result.success();
    }

    /**
     * 判断当前用户是否为管理员（SUPER_ADMIN 或 ADMIN）
     * Day 5 实现 @RequireRole 注解后将替换此手动判断
     */
    private boolean isManager() {
        String role = UserContext.getRole();
        return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
    }
}
