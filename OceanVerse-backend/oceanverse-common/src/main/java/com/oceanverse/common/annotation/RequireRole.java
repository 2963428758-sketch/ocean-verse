package com.oceanverse.common.annotation;

import java.lang.annotation.*;

/**
 * 角色权限注解 — 声明式角色控制
 * <p>
 * 标注在 Controller 方法或类上，要求调用者拥有指定角色。
 * 默认逻辑为 OR（拥有任一角色即可放行）。
 * <p>
 * 当与 @RequirePermission 同时使用时，优先校验角色：
 * 若角色匹配则直接放行，否则再校验权限码。
 * <p>
 * 超级管理员（SUPER_ADMIN）自动放行，无需匹配具体角色。
 *
 * @author OceanVerse
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 所需角色标识列表（OR 逻辑）
     * 例如: {"SUPER_ADMIN", "ADMIN"}
     */
    String[] value();
}
