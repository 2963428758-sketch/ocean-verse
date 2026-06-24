package com.oceanverse.visual.controller;

import com.oceanverse.auth.context.UserContext;
import com.oceanverse.common.annotation.RequireRole;
import com.oceanverse.common.result.Result;
import com.oceanverse.visual.dto.ExportQueryDTO;
import com.oceanverse.visual.mapper.VisualMapper;
import com.oceanverse.visual.service.VisualExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据导出接口
 * <p>
 * 鉴权：需要登录（WebConfig 中 /api/visual/export/** 不在放行列表）。
 * 角色区分：all=true 时仅管理员可用，Service 层二次校验。
 */
@Slf4j
@RestController
@RequestMapping("/api/visual/export")
@RequiredArgsConstructor
@RequireRole({"SUPER_ADMIN", "ADMIN", "RESEARCHER", "OBSERVER"})
public class VisualExportController {

    private final VisualExportService visualExportService;
    private final VisualMapper visualMapper;

    /**
     * 导出物种分布数据
     */
    @GetMapping("/species")
    public void exportSpecies(ExportQueryDTO query, HttpServletResponse response) {
        checkAllPermission(query);
        log.info("导出物种分布数据: userId={}, all={}", UserContext.getUserId(), query.getAll());
        visualExportService.exportSpecies(query, response);
    }

    /**
     * 导出观测记录数据
     */
    @GetMapping("/observation")
    public void exportObservation(ExportQueryDTO query, HttpServletResponse response) {
        checkAllPermission(query);
        log.info("导出观测记录数据: userId={}, all={}", UserContext.getUserId(), query.getAll());
        visualExportService.exportObservation(query, response);
    }

    /**
     * 导出统计汇总数据（多 Sheet）
     */
    @GetMapping("/statistics")
    public void exportStatistics(ExportQueryDTO query, HttpServletResponse response) {
        log.info("导出统计汇总数据: userId={}", UserContext.getUserId());
        visualExportService.exportStatistics(query, response);
    }

    // ==================== 数据预览（带筛选） ====================

    /**
     * 预览观测记录（前 20 条，JSON 格式）
     */
    @GetMapping("/observation/preview")
    public Result<List<Map<String, Object>>> previewObservation(
            @RequestParam(required = false) String observationType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<String> types = parseList(observationType);
        List<Map<String, Object>> data = visualMapper.getObservationsForExport(types, startDate, endDate);
        List<Map<String, Object>> preview = data.stream().limit(20).collect(Collectors.toList());
        return Result.success(preview);
    }

    private List<String> parseList(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== 鉴权校验 ====================

    /**
     * 检查全量导出权限：all=true 时仅管理员可用
     */
    private void checkAllPermission(ExportQueryDTO query) {
        if (Boolean.TRUE.equals(query.getAll()) && !UserContext.isSuperAdmin() && !UserContext.hasAnyRole("ADMIN")) {
            throw new com.oceanverse.common.exception.BusinessException(403, "无权限导出全量数据，仅管理员可使用此功能");
        }
    }
}
