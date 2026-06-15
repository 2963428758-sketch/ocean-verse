package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户积分记录
 */
@Data
@TableName("user_points")
public class UserPoints {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 积分变动（正数增加，负数扣减） */
    private Integer points;
    /** 行为类型: OBSERVATION, RECOGNITION, POST, DAILY_LOGIN */
    private String actionType;
    private String description;
    private LocalDateTime createTime;
}
