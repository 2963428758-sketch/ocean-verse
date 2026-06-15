package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 观测记录实体
 */
@Data
@TableName("observation")
public class Observation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String observationCode;
    private String observationType;
    private java.time.LocalDate observationDate;
    private java.time.LocalTime observationTime;
    private Integer durationMinutes;
    private Long locationId;
    private Long ecosystemId;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private java.math.BigDecimal depth;
    private java.math.BigDecimal waterTemperature;
    private java.math.BigDecimal salinity;
    private java.math.BigDecimal ph;
    private String weatherCondition;
    private String seaCondition;
    private Long observerId;
    private String observerName;
    private String organization;
    private String equipmentUsed;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
