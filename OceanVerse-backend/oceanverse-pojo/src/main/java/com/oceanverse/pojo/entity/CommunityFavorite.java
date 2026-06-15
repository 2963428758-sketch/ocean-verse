package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收藏实体
 */
@Data
@TableName("community_favorite")
public class CommunityFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long targetId;
    /** 目标类型: SPECIES, POST, OBSERVATION */
    private String targetType;
    private LocalDateTime createTime;
}
