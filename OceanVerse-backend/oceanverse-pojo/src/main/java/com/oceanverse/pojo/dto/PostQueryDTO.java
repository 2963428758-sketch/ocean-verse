package com.oceanverse.pojo.dto;

import lombok.Data;

@Data
public class PostQueryDTO {
    private String postType;
    private Long userId;
    private Long speciesId;
    /** 排序: LATEST, HOT */
    private String orderBy;
    /** 仅查看自己的待审核帖子 */
    private Boolean myPending;
    private Integer page = 1;
    private Integer size = 10;
}
