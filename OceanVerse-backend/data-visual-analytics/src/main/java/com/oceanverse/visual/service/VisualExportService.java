package com.oceanverse.visual.service;

import com.oceanverse.visual.dto.ExportQueryDTO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 数据导出 Service
 */
public interface VisualExportService {

    /**
     * 导出物种分布数据
     */
    void exportSpecies(ExportQueryDTO query, HttpServletResponse response);

    /**
     * 导出观测记录数据
     */
    void exportObservation(ExportQueryDTO query, HttpServletResponse response);

    /**
     * 导出统计汇总数据（多 Sheet：IUCN 等级 / 科分布 / 观测趋势）
     */
    void exportStatistics(ExportQueryDTO query, HttpServletResponse response);
}
