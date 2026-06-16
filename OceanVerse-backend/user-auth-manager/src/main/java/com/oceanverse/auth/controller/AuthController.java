package com.oceanverse.auth.controller;

import com.oceanverse.auth.context.UserContext;
import com.oceanverse.auth.service.UserService;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.LoginDTO;
import com.oceanverse.pojo.dto.RegisterDTO;
import com.oceanverse.pojo.vo.LoginVO;
import com.oceanverse.pojo.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return Result.success(userService.login(dto, clientIp));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        userService.logout(token);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo() {
        Long userId = UserContext.getUserId();
        return Result.success(userService.getUserInfo(userId));
    }

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
            // 多级代理时取第一个 IP
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
