package com.oceanverse.auth.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
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
import java.util.regex.Pattern;

/**
 * 数据权限拦截器 — MyBatis-Plus InnerInterceptor
 */
@Slf4j
public class DataScopeInterceptor implements InnerInterceptor {

    private static final Map<String, String> TABLE_USER_COLUMN = Map.of(
            "community_post", "user_id",
            "community_comment", "user_id",
            "image_recognition", "user_id",
            "qa_history", "user_id",
            "user_points", "user_id",
            "species", "create_by"
    );

    private static final Map<String, Pattern> TABLE_PATTERNS = TABLE_USER_COLUMN.keySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                    t -> t,
                    t -> Pattern.compile("\\b" + Pattern.quote(t) + "\\b", Pattern.CASE_INSENSITIVE)
            ));

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms,
                            Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return;
        }

        // 跳过 COUNT 查询
        String sql = boundSql.getSql().trim();
        if (sql.toUpperCase().startsWith("SELECT COUNT")) {
            return;
        }

        // 跳过单条记录查询（selectById、selectOne等）
        if (isSingleRecordQuery(ms)) {
            return;
        }

        UserContext.UserInfo userInfo = UserContext.get();
        if (userInfo == null || userInfo.getUserId() == null) {
            return;
        }

        if (UserContext.isSuperAdmin()) {
            return;
        }

        Integer dataScope = userInfo.getDataScope();
        if (dataScope == null || dataScope == CommonConstants.DATA_SCOPE_ALL) {
            return;
        }

        String originalSql = boundSql.getSql();

        boolean matched = false;
        for (Map.Entry<String, Pattern> entry : TABLE_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(originalSql).find()) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return;
        }

        StringBuilder newSql = new StringBuilder();
        newSql.append("SELECT * FROM (");
        newSql.append(originalSql);
        newSql.append(") _ds_tmp WHERE 1=1");

        for (Map.Entry<String, String> entry : TABLE_USER_COLUMN.entrySet()) {
            if (TABLE_PATTERNS.get(entry.getKey()).matcher(originalSql).find()) {
                // 对于 community_comment 表，如果 SQL 中已有 post_id 条件，跳过 user_id 过滤
                if ("community_comment".equals(entry.getKey())
                        && Pattern.compile("\\bpost_id\\b", Pattern.CASE_INSENSITIVE).matcher(originalSql).find()) {
                    continue;
                }
                newSql.append(" AND ").append(entry.getValue())
                        .append(" = ").append(userInfo.getUserId());
            }
        }

        PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
        mpBoundSql.sql(newSql.toString());

        log.debug("数据权限拦截生效: userId={}, dataScope={}", userInfo.getUserId(), dataScope);
    }

    private boolean isSingleRecordQuery(MappedStatement ms) {
        String id = ms.getId();
        int lastDot = id.lastIndexOf('.');
        String methodName = id.substring(lastDot + 1);
        return methodName.startsWith("selectById")
                || methodName.startsWith("selectOne")
                || methodName.startsWith("selectByMap");
    }
}
