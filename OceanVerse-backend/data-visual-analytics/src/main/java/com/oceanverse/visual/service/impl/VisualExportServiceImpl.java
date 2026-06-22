package com.oceanverse.visual.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.oceanverse.auth.context.UserContext;
import com.oceanverse.visual.dto.ExportObservationDTO;
import com.oceanverse.visual.dto.ExportQueryDTO;
import com.oceanverse.visual.dto.ExportSpeciesDTO;
import com.oceanverse.visual.mapper.VisualMapper;
import com.oceanverse.visual.service.VisualExportService;
import com.oceanverse.visual.service.VisualService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据导出 Service 实现 — 使用 EasyExcel 写流输出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisualExportServiceImpl implements VisualExportService {

    private final VisualMapper visualMapper;
    private final VisualService visualService;

    @Override
    public void exportSpecies(ExportQueryDTO query, HttpServletResponse response) {
        List<Map<String, Object>> rawData;

        if (Boolean.TRUE.equals(query.getAll()) && isAdmin()) {
            // 管理员导出全量
            rawData = visualMapper.getSpeciesDistributionAll();
        } else {
            // 按筛选导出
            List<String> iucnList = parseList(query.getIucnStatus());
            List<String> familyList = parseList(query.getFamily());
            rawData = visualMapper.getSpeciesDistributionForExport(iucnList, familyList);
        }

        List<ExportSpeciesDTO> dataList = rawData.stream().map(this::mapToSpeciesDTO).collect(Collectors.toList());
        String fileName = "物种分布数据_" + System.currentTimeMillis();
        writeExcel(response, fileName, "物种分布", ExportSpeciesDTO.class, dataList, query.getFormat());
    }

    @Override
    public void exportObservation(ExportQueryDTO query, HttpServletResponse response) {
        List<Map<String, Object>> rawData;

        if (Boolean.TRUE.equals(query.getAll()) && isAdmin()) {
            rawData = visualMapper.getAllObservationsForExport();
        } else {
            List<String> types = parseList(query.getObservationType());
            rawData = visualMapper.getObservationsForExport(types, query.getStartDate(), query.getEndDate());
        }

        List<ExportObservationDTO> dataList = rawData.stream().map(this::mapToObservationDTO).collect(Collectors.toList());
        String fileName = "观测记录数据_" + System.currentTimeMillis();
        writeExcel(response, fileName, "观测记录", ExportObservationDTO.class, dataList, query.getFormat());
    }

    @Override
    public void exportStatistics(ExportQueryDTO query, HttpServletResponse response) {
        // 获取三组统计数据
        List<Map<String, Object>> iucnData = visualService.getSpeciesByIucn();
        List<Map<String, Object>> familyData = visualService.getSpeciesByFamily();
        List<Map<String, Object>> trendData = visualService.getObservationTrend(
                query.getPeriod() != null ? query.getPeriod() : "monthly");

        String fileName = "统计汇总数据_" + System.currentTimeMillis();
        String format = query.getFormat() != null ? query.getFormat() : "excel";

        try {
            setResponseHeaders(response, fileName, format);
            OutputStream out = response.getOutputStream();

            if ("csv".equalsIgnoreCase(format)) {
                // CSV：写入 BOM 头后逐 sheet 写 CSV
                out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); // UTF-8 BOM
                writeCsvSheet(out, "IUCN保护等级分布", iucnData);
                out.write("\n\n".getBytes(StandardCharsets.UTF_8));
                writeCsvSheet(out, "物种按科分布", familyData);
                out.write("\n\n".getBytes(StandardCharsets.UTF_8));
                writeCsvSheet(out, "观测趋势", trendData);
            } else {
                // Excel：多 Sheet 写入
                ExcelWriter writer = EasyExcel.write(out).build();
                WriteSheet sheet1 = EasyExcel.writerSheet(0, "IUCN保护等级分布")
                        .head(buildHead(iucnData)).build();
                writer.write(buildData(iucnData), sheet1);

                WriteSheet sheet2 = EasyExcel.writerSheet(1, "物种按科分布")
                        .head(buildHead(familyData)).build();
                writer.write(buildData(familyData), sheet2);

                WriteSheet sheet3 = EasyExcel.writerSheet(2, "观测趋势")
                        .head(buildHead(trendData)).build();
                writer.write(buildData(trendData), sheet3);

                writer.finish();
            }
            log.info("统计汇总数据导出完成: iucn={}, family={}, trend={}", iucnData.size(), familyData.size(), trendData.size());
        } catch (Exception e) {
            log.error("导出统计汇总数据失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    private <T> void writeExcel(HttpServletResponse response, String fileName, String sheetName,
                                 Class<T> clazz, List<T> data, String format) {
        try {
            setResponseHeaders(response, fileName, format);
            OutputStream out = response.getOutputStream();

            if ("csv".equalsIgnoreCase(format)) {
                // CSV 写入（带 BOM 防中文乱码）
                out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                EasyExcel.write(out, clazz).sheet(sheetName).doWrite(data);
            } else {
                EasyExcel.write(out, clazz).sheet(sheetName).doWrite(data);
            }
            log.info("数据导出完成: file={}, rows={}", fileName, data.size());
        } catch (Exception e) {
            log.error("导出数据失败: file={}", fileName, e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    private void setResponseHeaders(HttpServletResponse response, String fileName, String format) {
        String extension = "csv".equalsIgnoreCase(format) ? "csv" : "xlsx";
        String contentType = "csv".equalsIgnoreCase(format)
                ? "text/csv;charset=UTF-8"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        try {
            String encoded = URLEncoder.encode(fileName + "." + extension, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + encoded);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        } catch (Exception e) {
            response.setHeader("Content-Disposition", "attachment;filename=export." + extension);
        }
    }

    /**
     * CSV 写入 Map 数据（动态表头）
     */
    private void writeCsvSheet(OutputStream out, String sheetName, List<Map<String, Object>> data) throws Exception {
        if (data == null || data.isEmpty()) {
            out.write((sheetName + "\n(无数据)\n").getBytes(StandardCharsets.UTF_8));
            return;
        }
        // 表头
        List<String> headers = new ArrayList<>(data.get(0).keySet());
        out.write(String.join(",", headers).getBytes(StandardCharsets.UTF_8));
        out.write("\n".getBytes(StandardCharsets.UTF_8));
        // 数据行
        for (Map<String, Object> row : data) {
            List<String> values = headers.stream()
                    .map(h -> {
                        Object v = row.get(h);
                        return v == null ? "" : "\"" + v.toString().replace("\"", "\"\"") + "\"";
                    })
                    .collect(Collectors.toList());
            out.write(String.join(",", values).getBytes(StandardCharsets.UTF_8));
            out.write("\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 构建 EasyExcel 动态表头（List<List<String>>）
     */
    private List<List<String>> buildHead(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return List.of(List.of("无数据"));
        }
        return data.get(0).keySet().stream()
                .map(List::of)
                .collect(Collectors.toList());
    }

    /**
     * 将 List<Map> 转为 List<List<Object>> 供 EasyExcel 写入
     */
    private List<List<Object>> buildData(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return List.of();
        }
        List<String> headers = new ArrayList<>(data.get(0).keySet());
        return data.stream()
                .map(row -> headers.stream()
                        .map(h -> (Object) (row.get(h) == null ? "" : row.get(h).toString()))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    private ExportSpeciesDTO mapToSpeciesDTO(Map<String, Object> map) {
        ExportSpeciesDTO dto = new ExportSpeciesDTO();
        dto.setId(toLong(map.get("id")));
        dto.setChineseName(toStr(map.get("chinese_name")));
        dto.setScientificName(toStr(map.get("scientific_name")));
        dto.setFamily(toStr(map.get("family")));
        dto.setIucnStatus(toStr(map.get("iucn_status")));
        dto.setCountry(toStr(map.get("country")));
        dto.setProvince(toStr(map.get("province")));
        dto.setRegionName(toStr(map.get("region_name")));
        dto.setLatitude(toStr(map.get("latitude")));
        dto.setLongitude(toStr(map.get("longitude")));
        dto.setDistributionType(toStr(map.get("distribution_type")));
        dto.setHabitatType(toStr(map.get("habitat_type")));
        dto.setDescription(toStr(map.get("description")));
        return dto;
    }

    private ExportObservationDTO mapToObservationDTO(Map<String, Object> map) {
        ExportObservationDTO dto = new ExportObservationDTO();
        dto.setId(toLong(map.get("id")));
        dto.setObservationCode(toStr(map.get("observation_code")));
        dto.setObservationType(toStr(map.get("observation_type")));
        dto.setObservationDate(toStr(map.get("observation_date")));
        dto.setObservationTime(toStr(map.get("observation_time")));
        Object dur = map.get("duration_minutes");
        dto.setDurationMinutes(dur != null ? ((Number) dur).intValue() : null);
        dto.setLatitude(toStr(map.get("latitude")));
        dto.setLongitude(toStr(map.get("longitude")));
        dto.setDepth(toStr(map.get("depth")));
        dto.setWaterTemperature(toStr(map.get("water_temperature")));
        dto.setSalinity(toStr(map.get("salinity")));
        dto.setPh(toStr(map.get("ph")));
        dto.setWeatherCondition(toStr(map.get("weather_condition")));
        dto.setSeaCondition(toStr(map.get("sea_condition")));
        dto.setObserverName(toStr(map.get("observer_name")));
        dto.setOrganization(toStr(map.get("organization")));
        dto.setEquipmentUsed(toStr(map.get("equipment_used")));
        dto.setNotes(toStr(map.get("notes")));
        return dto;
    }

    private List<String> parseList(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean isAdmin() {
        return UserContext.isSuperAdmin() || UserContext.hasAnyRole("ADMIN");
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private String toStr(Object o) {
        return o == null ? "" : o.toString();
    }
}
