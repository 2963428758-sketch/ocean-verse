package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 观测地点实体
 */
@Data
@TableName("observation_location")
public class ObservationLocation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String locationCode;
    private String locationName;
    private String locationType;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String country;
    private String province;
    private String city;
    private Long ecosystemId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic(delval = "UNIX_TIMESTAMP()")
    private Long deleted;
}
