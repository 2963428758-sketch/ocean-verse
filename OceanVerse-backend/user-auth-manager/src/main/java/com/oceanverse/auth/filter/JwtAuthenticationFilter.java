package com.oceanverse.auth.filter;

import com.oceanverse.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器 — 在 Spring Security 过滤链中解析 JWT 并填充 SecurityContext
 * <p>
 * 解决 Spring Security `.anyRequest().authenticated()` 在 JwtInterceptor 之前运行导致的 403 问题。
 * 本过滤器在 Spring Security 授权检查之前执行，确保合法 JWT 请求能通过认证。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // 无 Token 或格式不正确 → 不设置 SecurityContext，交给后续过滤器处理
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            // 1. 校验 JWT 签名和有效期
            if (!JwtUtil.validate(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 校验 Token 类型（必须是 AccessToken）
            if (!JwtUtil.isAccessToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 解析 JWT
            Claims claims = JwtUtil.parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            // 4. 构建 Authentication 对象
            List<SimpleGrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // 5. 设置 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Token 解析失败 → 不设置 SecurityContext，后续会被拒绝
            log.warn("JWT 解析失败: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
