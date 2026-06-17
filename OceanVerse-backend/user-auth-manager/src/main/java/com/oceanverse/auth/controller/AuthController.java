package com.oceanverse.auth.controller;

import com.oceanverse.auth.context.UserContext;
import com.oceanverse.auth.service.UserService;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.LoginDTO;
import com.oceanverse.pojo.dto.RegisterDTO;
import com.oceanverse.pojo.vo.LoginVO;
import com.oceanverse.pojo.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口 — 登录、注册、退出、刷新 Token
 * <p>
 * 登录/注册/刷新为公开接口（已在 WebConfig 中 exclude）。
 * 退出和获取用户信息需要登录。
 * <p>
 * Swagger Tag: Auth（认证）
 *
 * @author OceanVerse
 */
@Tag(name = "Auth", description = "认证接口 — 登录、注册、退出、Token刷新")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户登录", description = "用户名密码登录，返回双 Token + 用户基本信息")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return Result.success(userService.login(dto, clientIp));
    }

    @Operation(summary = "用户注册", description = "新用户注册，自动分配默认角色 VIEWER")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @Operation(summary = "退出登录", description = "退出登录，Token 加入黑名单")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        userService.logout(token);
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息", description = "返回当前登录用户的详细信息，含角色和权限列表")
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo() {
        Long userId = UserContext.getUserId();
        return Result.success(userService.getUserInfo(userId));
    }

    @Operation(summary = "刷新 Token", description = "使用 RefreshToken 获取新的 AccessToken")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return Result.fail("refreshToken 不能为空");
        }
        return Result.success(userService.refreshToken(refreshToken));
    }

    /**
     * 获取客户端真实 IP（支持反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
