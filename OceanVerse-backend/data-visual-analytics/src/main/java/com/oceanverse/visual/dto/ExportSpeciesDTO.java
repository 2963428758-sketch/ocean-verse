package com.oceanverse.visual.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 物种分布导出 DTO（EasyExcel 表头定义）
 */
@Data
public class ExportSpeciesDTO {

    @ExcelProperty("ID")
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty("中文名")
    @ColumnWidth(20)
    private String chineseName;

    @ExcelProperty("学名")
    @ColumnWidth(25)
    private String scientificName;

    @ExcelProperty("科")
    @ColumnWidth(15)
    private String family;

    @ExcelProperty("IUCN等级")
    @ColumnWidth(12)
    private String iucnStatus;

    @ExcelProperty("国家")
    @ColumnWidth(15)
    private String country;

    @ExcelProperty("省份")
    @ColumnWidth(15)
    private String province;

    @ExcelProperty("区域")
    @ColumnWidth(20)
    private String regionName;

    @ExcelProperty("纬度")
    @ColumnWidth(12)
    private String latitude;

    @ExcelProperty("经度")
    @ColumnWidth(12)
    private String longitude;

    @ExcelProperty("分布类型")
    @ColumnWidth(15)
    private String distributionType;

    @ExcelProperty("栖息地类型")
    @ColumnWidth(15)
    private String habitatType;

    @ExcelProperty("描述")
    @ColumnWidth(40)
    private String description;
}
