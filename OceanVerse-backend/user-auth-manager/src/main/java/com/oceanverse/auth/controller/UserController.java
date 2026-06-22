package com.oceanverse.auth.controller;

import com.oceanverse.auth.context.UserContext;
import com.oceanverse.auth.service.UserService;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.result.PageResult;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.UpdatePasswordDTO;
import com.oceanverse.pojo.dto.UpdateProfileDTO;
import com.oceanverse.pojo.dto.UploadResult;
import com.oceanverse.pojo.entity.SysLoginLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户自助接口 — 修改个人信息、修改密码
 * <p>
 * 仅需登录即可使用，无需额外角色或权限注解。
 * Swagger Tag: User（用户自助）
 *
 * @author OceanVerse
 */
@Tag(name = "User", description = "用户自助接口 — 个人信息、密码管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "修改个人信息", description = "修改昵称、邮箱、手机号、头像等个人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        Long userId = UserContext.getUserId();
        userService.updateProfile(userId, dto);
        return Result.success();
    }

    @Operation(summary = "修改密码", description = "修改密码后所有 Token 失效，需重新登录")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        Long userId = UserContext.getUserId();
        userService.updatePassword(userId, dto);
        return Result.success();
    }

    @Operation(summary = "上传头像", description = "上传用户头像图片，支持 jpg/png/gif/webp，不超过2MB")
    @PostMapping("/avatar")
    public Result<UploadResult> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        return Result.success(userService.uploadAvatar(userId, file));
    }

    @Operation(summary = "注销账号", description = "软删除账号，清除所有缓存和Token，需重新登录后将无法使用")
    @DeleteMapping("/account")
    public Result<Void> deleteAccount() {
        Long userId = UserContext.getUserId();
        userService.deleteAccount(userId);
        return Result.success();
    }

    @Operation(summary = "登录历史", description = "查询当前用户的登录历史记录")
    @GetMapping("/login-history")
    public Result<PageResult<SysLoginLog>> getLoginHistory(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, CommonConstants.MAX_PAGE_SIZE);
        Long userId = UserContext.getUserId();
        return Result.success(userService.getLoginHistory(userId, page, size));
    }
}
