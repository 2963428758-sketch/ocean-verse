package com.oceanverse.common.annotation;

import java.lang.annotation.*;

/**
 * 跳过数据权限拦截注解
 * <p>
 * 标注在 Controller 方法上，该方法的 SQL 查询将跳过 DataScopeInterceptor 的数据权限过滤。
 * 适用于需要查看所有用户数据的场景，如查看帖子评论列表、查看帖子详情等。
 *
 * @author OceanVerse
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkipDataScope {
}
