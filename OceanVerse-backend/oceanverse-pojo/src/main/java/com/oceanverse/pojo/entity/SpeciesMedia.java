package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 物种媒体资源实体
 */
@Data
@TableName("species_media")
public class SpeciesMedia {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long speciesId;
    /** 媒体类型: IMAGE/VIDEO/AUDIO */
    private String mediaType;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String thumbnailUrl;
    private String mediaTitle;
    private String mediaDescription;
    /** 是否为主图/主视频 */
    private Integer isPrimary;
    /** 状态: 0-隐藏, 1-正常 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Long deleted;
}
