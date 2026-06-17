package com.oceanverse.pojo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ObservationQueryDTO {
    /** 关键词（编号/观测员名称模糊匹配） */
    private String keyword;
    /** 观测类型: DIVE/SURVEY/SIGHTING/TRACKING */
    private String observationType;
    /** 开始日期 */
    private LocalDate startDate;
    /** 结束日期 */
    private LocalDate endDate;
    /** 生态系统ID */
    private Long ecosystemId;
    /** 观测地点ID */
    private Long locationId;
    /** 观测员姓名 */
    private String observerName;
    /** 排序方式: createTime(默认) / observationCode / observationDate */
    private String sort = "createTime";
    /** 页码，默认1 */
    private Integer page = 1;
    /** 每页条数，默认10 */
    private Integer size = 10;
}
