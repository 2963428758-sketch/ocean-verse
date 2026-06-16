package com.oceanverse.auth.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanverse.auth.context.UserContext;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.result.Result;
import com.oceanverse.common.utils.JwtUtil;
import com.oceanverse.common.utils.RedisUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.List;

/**
 * JWT 拦截器 — 解析 Token 并构建 UserContext
 * <p>
 * 设计为"可选认证"模式：
 * - 有 Token 且有效 → 解析并设置 UserContext
 * - 有 Token 但无效 → 返回 401（明确拒绝）
 * - 无 Token → 不设置 UserContext，放行给 PermissionInterceptor 决定是否拒绝
 * <p>
 * 这样公开接口（如 GET /api/species/1）无需 Token 也能访问，
 * 而需要登录的接口由 PermissionInterceptor 通过注解校验拦截。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");

        // 无 Token → 放行（由 PermissionInterceptor 根据注解决定是否需要登录）
        if (header == null || !header.startsWith("Bearer ")) {
            return true;
        }

        String token = header.substring(7);

        // 1. 校验 JWT 签名和有效期
        if (!JwtUtil.validate(token)) {
            writeUnauthorized(response, "Token 已过期或无效");
            return false;
        }

        // 2. 校验 Token 类型
        if (!JwtUtil.isAccessToken(token)) {
            writeUnauthorized(response, "Token 类型错误，需要 AccessToken");
            return false;
        }

        // 3. 校验 Token 黑名单
        String tokenHash = DigestUtils.md5DigestAsHex(token.getBytes());
        if (Boolean.TRUE.equals(redisUtil.hasKey(CommonConstants.REDIS_TOKEN_BLACKLIST + tokenHash))) {
            writeUnauthorized(response, "Token 已失效，请重新登录");
            return false;
        }

        // 4. 解析 JWT（只解析一次，复用 Claims）
        Claims claims = JwtUtil.parseToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        Integer dataScope = claims.get("dataScope", Integer.class);

        // 5. 构建 UserContext
        UserContext.UserInfo userInfo = new UserContext.UserInfo();
        userInfo.setUserId(userId);
        userInfo.setUsername(username);
        userInfo.setRole(role);
        userInfo.setDataScope(dataScope != null ? dataScope : CommonConstants.DATA_SCOPE_SELF);

        // 6. 从 Redis 加载权限缓存
        @SuppressWarnings("unchecked")
        List<String> permissions = redisUtil.getObject(
                CommonConstants.REDIS_USER_PERMS + userId, List.class);
        userInfo.setPermissions(permissions != null ? permissions : Collections.emptyList());

        // 7. 从 Redis 加载角色列表缓存
        @SuppressWarnings("unchecked")
        List<String> roles = redisUtil.getObject(
                CommonConstants.REDIS_USER_ROLES + userId, List.class);
        if (roles != null && !roles.isEmpty()) {
            userInfo.setRoles(roles);
        } else {
            userInfo.setRoles(role != null ? List.of(role) : Collections.emptyList());
        }

        UserContext.set(userInfo);
        log.debug("JwtInterceptor: method={}, uri={}, userId={}, role={}", request.getMethod(), request.getRequestURI(), userId, role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.error(CommonConstants.CODE_UNAUTHORIZED, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
