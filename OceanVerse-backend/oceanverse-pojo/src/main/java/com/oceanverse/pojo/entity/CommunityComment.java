package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论实体
 */
@Data
@TableName("community_comment")
public class CommunityComment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long userId;
    /** 父评论ID，null表示顶级评论 */
    private Long parentId;
    private String content;
    private Integer likeCount;
    /** 状态: 0-隐藏, 1-正常 */
    private Integer status;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;

    /** 非数据库字段，由 service 填充 */
    @TableField(exist = false)
    private String username;
    /** 用户头像URL */
    @TableField(exist = false)
    private String avatarUrl;
}
