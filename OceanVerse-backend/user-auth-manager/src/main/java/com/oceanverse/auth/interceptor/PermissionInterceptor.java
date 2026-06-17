package com.oceanverse.auth.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.annotation.RequireRole;
import com.oceanverse.auth.context.UserContext;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限拦截器 — 基于注解的声明式权限控制
 * <p>
 * 拦截器执行顺序（固定）：
 * CorsInterceptor → JwtInterceptor → <b>PermissionInterceptor</b> → DataScopeInterceptor(MyBatis层)
 * <p>
 * 校验优先级：
 * 1. 超级管理员（SUPER_ADMIN）直接放行
 * 2. 先校验 @RequireRole — 角色匹配则直接放行
 * 3. 再校验 @RequirePermission — 权限码匹配则放行
 * 4. 两者都不满足 → 返回 403
 *
 * @author OceanVerse
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 非 Controller 方法直接放行（如静态资源）
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 超级管理员直接放行
        if (UserContext.isSuperAdmin()) {
            return true;
        }

        // 获取方法级注解（优先），无则获取类级注解
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireRole.class);
        }

        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            requirePermission = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
        }

        // 没有任何权限注解 → 无需校验，直接放行
        if (requireRole == null && requirePermission == null) {
            return true;
        }

        // 有权限注解但无 UserContext → 用户未登录，返回 401
        if (UserContext.getUserId() == null) {
            writeUnauthorized(response, "未登录或 token 无效");
            return false;
        }

        // 1. 优先校验角色
        if (requireRole != null) {
            String[] requiredRoles = requireRole.value();
            if (UserContext.hasAnyRole(requiredRoles)) {
                return true;
            }
        }

        // 2. 再校验权限码
        if (requirePermission != null) {
            String[] permCodes = requirePermission.value();
            boolean requireAll = requirePermission.requireAll();
            if (UserContext.hasPermissions(permCodes, requireAll)) {
                return true;
            }
        }

        // 3. 角色和权限都不满足 → 403
        log.warn("权限不足: userId={}, uri={}, method={}",
                UserContext.getUserId(), request.getRequestURI(), request.getMethod());
        writeForbidden(response, "没有权限执行此操作");
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.error(CommonConstants.CODE_UNAUTHORIZED, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.error(CommonConstants.CODE_FORBIDDEN, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
