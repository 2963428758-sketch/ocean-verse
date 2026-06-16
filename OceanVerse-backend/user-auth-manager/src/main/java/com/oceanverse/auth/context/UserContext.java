package com.oceanverse.auth.context;

import com.oceanverse.common.constants.CommonConstants;
import lombok.Data;

import java.util.List;

public class UserContext {

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    public static void set(UserInfo userInfo) {
        HOLDER.set(userInfo);
    }

    public static UserInfo get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        UserInfo info = HOLDER.get();
        return info != null ? info.getUserId() : null;
    }

    public static String getUsername() {
        UserInfo info = HOLDER.get();
        return info != null ? info.getUsername() : null;
    }

    /**
     * 获取用户首个（主要）角色标识
     */
    public static String getRole() {
        UserInfo info = HOLDER.get();
        return info != null ? info.getRole() : null;
    }

    /**
     * 获取用户所有角色标识列表
     */
    public static List<String> getRoles() {
        UserInfo info = HOLDER.get();
        return info != null ? info.getRoles() : List.of();
    }

    public static List<String> getPermissions() {
        UserInfo info = HOLDER.get();
        return info != null ? info.getPermissions() : List.of();
    }

    public static Integer getDataScope() {
        UserInfo info = HOLDER.get();
        return info != null ? info.getDataScope() : null;
    }

    /**
     * 判断当前用户是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        UserInfo info = HOLDER.get();
        if (info == null) {
            return false;
        }
        // 优先检查 roles 列表，兼容回退到单 role 字段
        if (info.getRoles() != null && !info.getRoles().isEmpty()) {
            return info.getRoles().contains(CommonConstants.ROLE_SUPER_ADMIN);
        }
        return CommonConstants.ROLE_SUPER_ADMIN.equals(info.getRole());
    }

    /**
     * 判断当前用户是否拥有指定角色（OR 逻辑）
     */
    public static boolean hasAnyRole(String... roles) {
        if (isSuperAdmin()) {
            return true;
        }
        UserInfo info = HOLDER.get();
        if (info == null) {
            return false;
        }
        List<String> userRoles = info.getRoles();
        if (userRoles == null || userRoles.isEmpty()) {
            // 回退到单角色字段
            String role = info.getRole();
            if (role == null) return false;
            for (String r : roles) {
                if (role.equals(r)) return true;
            }
            return false;
        }
        for (String r : roles) {
            if (userRoles.contains(r)) return true;
        }
        return false;
    }

    /**
     * 判断当前用户是否拥有指定权限（AND 或 OR）
     */
    public static boolean hasPermissions(String[] permCodes, boolean requireAll) {
        if (isSuperAdmin()) {
            return true;
        }
        UserInfo info = HOLDER.get();
        if (info == null || info.getPermissions() == null) {
            return false;
        }
        List<String> userPerms = info.getPermissions();
        if (requireAll) {
            for (String perm : permCodes) {
                if (!userPerms.contains(perm)) return false;
            }
            return true;
        } else {
            for (String perm : permCodes) {
                if (userPerms.contains(perm)) return true;
            }
            return false;
        }
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        /** 主要角色标识（来自 JWT，兼容旧逻辑） */
        private String role;
        /** 用户所有角色列表（从 Redis 缓存加载） */
        private List<String> roles;
        private List<String> permissions;
        private Integer dataScope;
    }
}
