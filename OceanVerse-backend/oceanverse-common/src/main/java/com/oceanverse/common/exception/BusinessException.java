package com.oceanverse.common.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException notFound(String resource) {
        return new BusinessException(404, resource + "不存在");
    }

    public static BusinessException unauthorized() {
        return new BusinessException(401, "未登录或登录已过期");
    }

    public static BusinessException forbidden() {
        return new BusinessException(403, "没有权限执行此操作");
    }

    public static BusinessException fail(String message) {
        return new BusinessException(400, message);
    }
}
