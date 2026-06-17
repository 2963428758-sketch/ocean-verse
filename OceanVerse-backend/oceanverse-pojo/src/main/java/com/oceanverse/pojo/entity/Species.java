package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 海洋物种实体
 */
@Data
@TableName("species")
public class Species {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String speciesCode;
    private String scientificName;
    private String commonName;
    private String chineseName;
    /** 保护等级: 1-一级, 2-二级, 3-三级 */
    private String protectionLevel;
    /** IUCN 状态 */
    private String iucnStatus;
    private String kingdom;
    private String phylum;
    private String className;
    private String orderName;
    private String family;
    private String genus;
    private String species;
    private String description;
    private String morphology;
    private String ecology;
    private String videoUrl;
    private Long conservationStatusId;
    private Integer isEndemic;
    private Integer isInvasive;
    private String dataQuality;
    private String source;
    /** 经度（非持久化字段，创建物种时用于生成初始分布记录） */
    @TableField(exist = false)
    private BigDecimal longitude;
    /** 纬度（非持久化字段，创建物种时用于生成初始分布记录） */
    @TableField(exist = false)
    private BigDecimal latitude;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    @TableLogic(delval = "#{id}")
    private Long deleted;
}
