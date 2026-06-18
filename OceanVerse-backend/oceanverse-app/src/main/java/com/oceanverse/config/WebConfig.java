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
 * 拦截器执行顺序：JwtInterceptor → PermissionInterceptor
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 第一层：JWT 认证
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
                        "/api/message/test/**"
                );

        // 第二层：角色/权限校验（@RequireRole、@RequirePermission）
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
