package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统通知实体
 */
@Data
@TableName("sys_notification")
public class SysNotification {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    /** 类型: SYSTEM, LIKE, COMMENT, FOLLOW */
    private String type;
    private Integer isRead;
    private Long relatedId;
    private Long targetPostId;
    private Long fromUserId;
    private LocalDateTime createTime;
}
