package com.oceanverse.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 * <p>
 * 本项目使用 JWT 拦截器 (JwtInterceptor) 做 API 认证，
 * Security 仅负责放行 Swagger 文档、静态资源等公开路径。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（REST API 不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用 Session（使用 JWT 无状态认证）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // Swagger / SpringDoc
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        // 公开 API 接口
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/captcha"
                        ).permitAll()
                        // 其余请求全部放行（由 JwtInterceptor 控制 API 权限）
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
