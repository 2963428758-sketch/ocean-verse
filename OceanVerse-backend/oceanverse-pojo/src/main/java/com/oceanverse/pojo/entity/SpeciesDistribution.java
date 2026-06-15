package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 物种分布实体
 */
@Data
@TableName("species_distribution")
public class SpeciesDistribution {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long speciesId;
    private String distributionType;
    private String regionName;
    private String country;
    private String province;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private java.math.BigDecimal depthMin;
    private java.math.BigDecimal depthMax;
    private String habitatType;
    private Integer isPrimary;
    private String populationEstimate;
    private String distributionStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
