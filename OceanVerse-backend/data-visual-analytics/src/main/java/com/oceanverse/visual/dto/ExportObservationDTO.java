package com.oceanverse.visual.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 观测记录导出 DTO（EasyExcel 表头定义）
 */
@Data
public class ExportObservationDTO {

    @ExcelProperty("ID")
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty("观测编号")
    @ColumnWidth(18)
    private String observationCode;

    @ExcelProperty("观测类型")
    @ColumnWidth(12)
    private String observationType;

    @ExcelProperty("观测日期")
    @ColumnWidth(14)
    private String observationDate;

    @ExcelProperty("观测时间")
    @ColumnWidth(12)
    private String observationTime;

    @ExcelProperty("时长(分钟)")
    @ColumnWidth(12)
    private Integer durationMinutes;

    @ExcelProperty("纬度")
    @ColumnWidth(12)
    private String latitude;

    @ExcelProperty("经度")
    @ColumnWidth(12)
    private String longitude;

    @ExcelProperty("水深(m)")
    @ColumnWidth(12)
    private String depth;

    @ExcelProperty("水温(℃)")
    @ColumnWidth(12)
    private String waterTemperature;

    @ExcelProperty("盐度")
    @ColumnWidth(10)
    private String salinity;

    @ExcelProperty("pH值")
    @ColumnWidth(10)
    private String ph;

    @ExcelProperty("天气")
    @ColumnWidth(15)
    private String weatherCondition;

    @ExcelProperty("海况")
    @ColumnWidth(15)
    private String seaCondition;

    @ExcelProperty("观测员")
    @ColumnWidth(12)
    private String observerName;

    @ExcelProperty("机构")
    @ColumnWidth(20)
    private String organization;

    @ExcelProperty("设备")
    @ColumnWidth(20)
    private String equipmentUsed;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String notes;
}
