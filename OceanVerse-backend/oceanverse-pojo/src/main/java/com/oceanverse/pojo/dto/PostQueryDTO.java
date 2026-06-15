package com.oceanverse.pojo.dto;

import lombok.Data;

@Data
public class PostQueryDTO {
    private String postType;
    private Long userId;
    private Long speciesId;
    /** 排序: LATEST, HOT */
    private String orderBy;
    private Integer page = 1;
    private Integer size = 10;
}
