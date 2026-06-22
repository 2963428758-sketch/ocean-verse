package com.oceanverse.visual.controller;

import com.oceanverse.common.result.Result;
import com.oceanverse.visual.service.VisualService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据可视化接口 — 成员C
 */
@RestController
@RequestMapping("/api/visual")
@RequiredArgsConstructor
public class VisualController {

    private final VisualService visualService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(visualService.getDashboardData());
    }

    @GetMapping("/species/distribution")
    public Result<List<Map<String, Object>>> speciesDistribution(
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) String iucnStatus,
            @RequestParam(required = false) String family) {
        return Result.success(visualService.getSpeciesDistribution(speciesId, iucnStatus, family));
    }

    @GetMapping("/trend/observation")
    public Result<List<Map<String, Object>>> observationTrend(
            @RequestParam(defaultValue = "monthly") String period) {
        return Result.success(visualService.getObservationTrend(period));
    }

    @GetMapping("/statistics/family")
    public Result<List<Map<String, Object>>> speciesByFamily() {
        return Result.success(visualService.getSpeciesByFamily());
    }

    @GetMapping("/statistics/iucn")
    public Result<List<Map<String, Object>>> speciesByIucnStatus() {
        return Result.success(visualService.getSpeciesByIucn());
    }
}
