package com.oceanverse.auth.config;

import com.oceanverse.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import jakarta.servlet.DispatcherType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 * <p>
 * 本项目使用 JWT 拦截器 (JwtInterceptor) 做 API 认证，
 * Security 仅负责放行 Swagger 文档、静态资源等公开路径。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（REST API 不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用 Session（使用 JWT 无状态认证）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 注册 JWT 过滤器（在 UsernamePasswordAuthenticationFilter 之前执行）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 配置请求授权规则（纵深防御层 — 与 JwtInterceptor 双重保护）
                .authorizeHttpRequests(auth -> auth
                        // SSE 异步分发需要 ASYNC dispatcher，Spring Security 6.x 默认拦截
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        // Swagger / SpringDoc
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        // 公开 API 接口（无需认证）
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/captcha"
                        ).permitAll()
                        // 公开查询接口（无需认证）
                        .requestMatchers(
                                "/api/species/list",
                                "/api/species/*",
                                "/api/species/statistics",
                                "/api/species/*/distributions",
                                "/api/community/post/list",
                                "/api/community/post/*",
                                "/api/visual/**",
                                "/api/message/test/**",
                                "/api/ai/**"
                        ).permitAll()
                        // WebSocket 端点
                        .requestMatchers("/ws/**").permitAll()
                        // 其余所有请求需要认证（JWT Header 必须存在）
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
