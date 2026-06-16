package com.oceanverse.visual.controller;

import com.oceanverse.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据可视化接口 — 成员C
 * <p>
 * 可视化接口均为公开接口（已在 WebConfig 中 exclude /api/visual/**）。
 * Swagger Tag: Visual（数据可视化）
 */
@Tag(name = "Visual", description = "数据可视化接口")
@RestController
@RequestMapping("/api/visual")
@RequiredArgsConstructor
public class VisualController {

    @Operation(summary = "仪表盘概览", description = "获取平台整体统计数据概览")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalSpecies", 0);
        data.put("totalObservations", 0);
        data.put("totalUsers", 0);
        data.put("totalRecognitions", 0);
        return Result.success(data);
    }

    @Operation(summary = "物种分布", description = "查询物种地理分布数据（经纬度列表）")
    @GetMapping("/species/distribution")
    public Result<Object> speciesDistribution(
            @Parameter(description = "物种ID") @RequestParam(required = false) Long speciesId) {
        return Result.success("物种分布数据接口待实现");
    }

    @Operation(summary = "观测趋势", description = "查询观测数据趋势")
    @GetMapping("/trend/observation")
    public Result<Object> observationTrend(
            @Parameter(description = "统计周期: monthly/yearly") @RequestParam(defaultValue = "monthly") String period) {
        return Result.success("观测趋势接口待实现");
    }

    @Operation(summary = "按科统计物种", description = "按生物分类科统计物种数量")
    @GetMapping("/statistics/family")
    public Result<Object> speciesByFamily() {
        return Result.success("按科统计接口待实现");
    }

    @Operation(summary = "按 IUCN 等级统计", description = "按 IUCN 保护等级统计物种数量")
    @GetMapping("/statistics/iucn")
    public Result<Object> speciesByIucnStatus() {
        return Result.success("IUCN统计接口待实现");
    }
}
