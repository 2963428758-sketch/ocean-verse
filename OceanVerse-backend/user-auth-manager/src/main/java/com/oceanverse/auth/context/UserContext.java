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

    public static String getRole() {
        UserInfo info = HOLDER.get();
        return info != null ? info.getRole() : null;
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
        return info != null && CommonConstants.ROLE_SUPER_ADMIN.equals(info.getRole());
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;
        private List<String> permissions;
        private Integer dataScope;
    }
}
