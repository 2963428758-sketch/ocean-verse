package com.oceanverse.pojo.dto;

import lombok.Data;

@Data
public class SpeciesQueryDTO {
    /** 关键词（学名/中英文名模糊匹配） */
    private String keyword;
    /** 科名 */
    private String family;
    /** IUCN 等级: LC/NT/VU/EN/CR/EW/EX */
    private String iucnStatus;
    /** 保护等级: 1-一级, 2-二级, 3-三级 */
    private String protectionLevel;
    /** 排序方式: createTime(默认) / speciesCode */
    private String sort = "createTime";
    /** 页码，默认1 */
    private Integer page = 1;
    /** 每页条数，默认10 */
    private Integer size = 10;
}
