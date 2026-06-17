package com.oceanverse.common.annotation;

import java.lang.annotation.*;

/**
 * 接口权限注解 — 声明式权限控制
 * <p>
 * 标注在 Controller 方法或类上，要求调用者拥有指定权限码。
 * 默认逻辑为 AND（需同时拥有所有权限码）。
 * <p>
 * 当与 @RequireRole 同时使用时，优先校验角色：
 * 若角色匹配则直接放行，否则再校验权限码。
 * <p>
 * 超级管理员（SUPER_ADMIN）自动放行，无需匹配具体权限。
 *
 * @author OceanVerse
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 所需权限码列表（AND 逻辑）
     * 例如: {"user:list", "user:create"}
     */
    String[] value();

    /**
     * 权限校验逻辑模式
     * true = AND（需拥有全部权限），false = OR（拥有任一权限即可）
     */
    boolean requireAll() default true;
}
