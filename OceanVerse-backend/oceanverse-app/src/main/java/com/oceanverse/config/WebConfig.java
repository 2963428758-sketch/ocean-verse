package com.oceanverse.config;

import com.oceanverse.auth.interceptor.JwtInterceptor;
import com.oceanverse.auth.interceptor.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置 — 拦截器、跨域
 * <p>
 * 拦截器执行顺序（固定，不可调换）：
 * 1. JwtInterceptor     — 解析 Token + Redis 黑名单 + 加载权限缓存 → UserContext
 * 2. PermissionInterceptor — @RequirePermission / @RequireRole 注解校验
 * <p>
 * DataScopeInterceptor 在 MyBatis 层生效（见 MyBatisPlusConfig），不在此注册。
 * <p>
 * Spring Security 仅作为被动壳（全 permitAll），所有鉴权由 Interceptor 层实现。
 * <p>
 * 注意：JwtInterceptor 的 excludePathPatterns 只排除真正不需要登录的公开接口。
 * 如果一个 URL 路径既有公开的 GET 接口，也有需要登录的 PUT/DELETE 接口，
 * 则不应排除 — 让 PermissionInterceptor 根据注解决定是否校验。
 * 对于没有 @RequirePermission 注解的公开方法，PermissionInterceptor 会自动放行。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. JWT 拦截器（必须最先执行，负责解析 Token 并构建 UserContext）
        registry.addInterceptor(jwtInterceptor)
                .order(1)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        // 认证相关公开接口
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/refresh",
                        // 物种列表（公开）
                        "/api/species/list",
                        // 社区公开接口
                        "/api/community/post/list",
                        "/api/community/leaderboard",
                        // 数据可视化全部公开
                        "/api/visual/**"
                );

        // 2. 权限拦截器（在 JWT 之后执行，依赖 UserContext 中的权限数据）
        // 无需 excludePathPatterns — 仅对有 @RequirePermission / @RequireRole 注解的方法生效
        registry.addInterceptor(permissionInterceptor)
                .order(2)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
