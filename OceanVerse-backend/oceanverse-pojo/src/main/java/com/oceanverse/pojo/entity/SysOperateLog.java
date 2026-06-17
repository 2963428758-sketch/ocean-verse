package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("sys_operation_log")
public class SysOperateLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块名 */
    private String module;

    /** 操作类型: CREATE/UPDATE/DELETE/QUERY/EXPORT */
    private String operationType;

    /** 请求URL */
    private String requestUrl;

    /** 请求方法: GET/POST/PUT/DELETE */
    private String requestMethod;

    /** 请求参数JSON */
    private String requestParams;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作IP */
    private String ipAddress;

    /** 执行耗时(ms) */
    private Integer executionTime;

    /** 操作结果: 1-成功, 0-失败 */
    private Integer operationResult;

    /** 错误信息 */
    private String errorMessage;

    private LocalDateTime createTime;
}
