package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 生态系统实体
 */
@Data
@TableName("ecosystem")
public class Ecosystem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ecosystemCode;
    private String ecosystemName;
    private String ecosystemType;
    private String description;
    private java.math.BigDecimal areaEstimate;
    private java.math.BigDecimal depthMin;
    private java.math.BigDecimal depthMax;
    private String temperatureRange;
    private String threatFactors;
    private String conservationStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic(delval = "UNIX_TIMESTAMP()")
    private Long deleted;
}
