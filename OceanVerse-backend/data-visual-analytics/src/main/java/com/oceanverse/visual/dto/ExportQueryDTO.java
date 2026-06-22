package com.oceanverse.visual.dto;

import lombok.Data;

/**
 * 数据导出请求参数
 */
@Data
public class ExportQueryDTO {

    /** 导出格式：excel / csv */
    private String format = "excel";

    /** 是否导出全量数据（仅管理员可用） */
    private Boolean all = false;

    // ===== 物种分布筛选 =====
    /** IUCN 等级筛选（逗号分隔，如 VU,EN） */
    private String iucnStatus;

    /** 科筛选（逗号分隔） */
    private String family;

    // ===== 观测记录筛选 =====
    /** 观测类型筛选（逗号分隔，如 DIVE,SURVEY） */
    private String observationType;

    /** 观测日期范围 - 开始 */
    private String startDate;

    /** 观测日期范围 - 结束 */
    private String endDate;

    // ===== 统计趋势筛选 =====
    /** 趋势粒度：monthly / weekly / daily */
    private String period = "monthly";
}
