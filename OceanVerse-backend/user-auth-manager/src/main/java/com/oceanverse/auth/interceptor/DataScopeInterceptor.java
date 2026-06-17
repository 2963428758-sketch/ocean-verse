package com.oceanverse.auth.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.oceanverse.auth.context.UserContext;
import com.oceanverse.common.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.Map;

/**
 * 数据权限拦截器 — MyBatis-Plus InnerInterceptor
 * <p>
 * 根据 UserContext 中的 dataScope 动态拼接 SQL 条件：
 * - dataScope = ALL（2）：不过滤
 * - dataScope = SELF（1）：追加 AND user_id = {当前用户ID}
 * - 超级管理员：不过滤
 * <p>
 * 仅拦截 SELECT 语句，且仅对配置的目标表生效。
 */
@Slf4j
public class DataScopeInterceptor implements InnerInterceptor {

    /**
     * 需要数据权限过滤的表名 → 用户ID列名
     * 按需扩展：其他模块的表加入此 Map 即可生效
     */
    private static final Map<String, String> TABLE_USER_COLUMN = Map.of(
            "community_post", "user_id",
            "community_comment", "user_id",
            "image_recognition", "user_id",
            "qa_history", "user_id",
            "user_points", "user_id",
            "species", "create_by"
    );

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms,
                            Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 仅拦截 SELECT
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return;
        }

        // 获取当前用户信息
        UserContext.UserInfo userInfo = UserContext.get();
        if (userInfo == null || userInfo.getUserId() == null) {
            return;
        }

        // 超级管理员不过滤
        if (CommonConstants.ROLE_SUPER_ADMIN.equals(userInfo.getRole())) {
            return;
        }

        // dataScope = ALL 不过滤
        Integer dataScope = userInfo.getDataScope();
        if (dataScope == null || dataScope == CommonConstants.DATA_SCOPE_ALL) {
            return;
        }

        // 获取原始 SQL
        String originalSql = boundSql.getSql();
        String sqlLower = originalSql.toLowerCase();

        // 检查 SQL 中是否包含目标表名
        boolean matched = false;
        for (String tableName : TABLE_USER_COLUMN.keySet()) {
            if (sqlLower.contains(tableName)) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return;
        }

        // 拼接数据权限条件
        StringBuilder newSql = new StringBuilder();
        newSql.append("SELECT * FROM (");
        newSql.append(originalSql);
        newSql.append(") _ds_tmp WHERE 1=1");

        for (Map.Entry<String, String> entry : TABLE_USER_COLUMN.entrySet()) {
            if (sqlLower.contains(entry.getKey())) {
                newSql.append(" AND ").append(entry.getValue())
                        .append(" = ").append(userInfo.getUserId());
            }
        }

        // 通过反射修改 BoundSql 中的 SQL
        try {
            var sqlField = BoundSql.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            sqlField.set(boundSql, newSql.toString());
        } catch (ReflectiveOperationException e) {
            log.error("数据权限SQL改写失败", e);
        }

        log.debug("数据权限拦截生效: userId={}, dataScope={}", userInfo.getUserId(), dataScope);
    }
}
