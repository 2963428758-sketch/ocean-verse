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
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录或 token 无效");
            return false;
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

        // 6. 从 Redis 加载权限缓存（Day 4 RBAC 实现后生效）
        @SuppressWarnings("unchecked")
        List<String> permissions = redisUtil.getObject(
                CommonConstants.REDIS_USER_PERMS + userId, List.class);
        userInfo.setPermissions(permissions != null ? permissions : Collections.emptyList());

        UserContext.set(userInfo);
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
