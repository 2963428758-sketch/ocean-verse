package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（登录失败时可能为空） */
    private Long userId;

    /** 登录用户名 */
    private String username;

    /** 登录IP */
    private String ipAddress;

    /** 浏览器 User-Agent */
    private String userAgent;

    /** 登录状态: 1-成功, 0-失败 */
    private Integer status;

    /** 结果说明 */
    private String message;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
