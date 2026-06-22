package com.oceanverse.visual.service;

import java.util.List;
import java.util.Map;

public interface VisualService {

    /** 仪表盘聚合统计 */
    Map<String, Object> getDashboardData();

    /** 物种地理分布（支持 IUCN / 科筛选） */
    List<Map<String, Object>> getSpeciesDistribution(Long speciesId, String iucnStatus, String family);

    /** 观测趋势 */
    List<Map<String, Object>> getObservationTrend(String period);

    /** 按科统计物种数量 */
    List<Map<String, Object>> getSpeciesByFamily();

    /** 按 IUCN 等级统计物种数量 */
    List<Map<String, Object>> getSpeciesByIucn();
}
