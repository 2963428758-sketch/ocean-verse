package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 关注关系实体
 */
@Data
@TableName("community_follow")
public class CommunityFollow {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关注者 */
    private Long userId;
    /** 被关注者 */
    private Long followUserId;
    private LocalDateTime createTime;
}
