package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 点赞实体
 */
@Data
@TableName("community_like")
public class CommunityLike {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long targetId;
    /** 目标类型: POST, COMMENT, SPECIES */
    private String targetType;
    private LocalDateTime createTime;
}
