package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区动态实体
 */
@Data
@TableName("community_post")
public class CommunityPost {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String content;
    /** 类型: NORMAL-普通, OBSERVATION-观测, RECOGNITION-识别 */
    private String postType;
    private Long relatedSpeciesId;
    private Long relatedObservationId;
    /** 图片URL列表 (JSON数组) */
    private String imageUrls;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    /** 状态: 0-隐藏, 1-正常, 2-置顶 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    /** 非数据库字段，由 service 填充 */
    @TableField(exist = false)
    private String username;
}
