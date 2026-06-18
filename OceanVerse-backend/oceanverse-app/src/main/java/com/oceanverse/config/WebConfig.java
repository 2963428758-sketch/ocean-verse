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
 * 拦截器执行顺序：JwtInterceptor（解析 Token + 加载权限缓存）
 * → PermissionInterceptor（@RequireRole / @RequirePermission 注解校验）
 * <p>
 * Security 仅作为被动壳（SecurityConfig 已全 permitAll），
 * 所有鉴权由拦截器层实现。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/species/list",
                        "/api/species/{id}",
                        "/api/species/statistics",
                        "/api/species/{id}/distributions",
                        "/api/community/post/list",
                        "/api/community/post/{id}",
                        "/api/visual/**",
                        "/api/message/test/**",
                        "/api/ai/**"
                );

        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/refresh",
                        "/api/species/list",
                        "/api/species/{id}",
                        "/api/species/statistics",
                        "/api/species/{id}/distributions",
                        "/api/community/post/list",
                        "/api/community/post/{id}",
                        "/api/visual/**",
                        "/api/message/test/**",
                        "/api/ai/**"
                );
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
