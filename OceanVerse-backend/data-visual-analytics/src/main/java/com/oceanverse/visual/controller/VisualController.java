package com.oceanverse.visual.controller;

import com.oceanverse.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
//我是郭志鹏的爸爸
/**
 * 数据可视化接口 — 成员C
 */
@RestController
@RequestMapping("/api/visual")
@RequiredArgsConstructor
public class VisualController {

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        // TODO: 从各模块聚合统计数据
        Map<String, Object> data = new HashMap<>();
        data.put("totalSpecies", 0);
        data.put("totalObservations", 0);
        data.put("totalUsers", 0);
        data.put("totalRecognitions", 0);
        return Result.success(data);
    }

    @GetMapping("/species/distribution")
    public Result<Object> speciesDistribution(@RequestParam(required = false) Long speciesId) {
        // TODO: 查询物种分布数据，返回经纬度列表
        return Result.success("物种分布数据接口待实现");
    }

    @GetMapping("/trend/observation")
    public Result<Object> observationTrend(
            @RequestParam(defaultValue = "monthly") String period) {
        // TODO: 观测趋势数据
        return Result.success("观测趋势接口待实现");
    }

    @GetMapping("/statistics/family")
    public Result<Object> speciesByFamily() {
        // TODO: 按科统计物种数量
        return Result.success("按科统计接口待实现");
    }

    @GetMapping("/statistics/iucn")
    public Result<Object> speciesByIucnStatus() {
        // TODO: 按 IUCN 保护等级统计
        return Result.success("IUCN统计接口待实现");
    }
}
