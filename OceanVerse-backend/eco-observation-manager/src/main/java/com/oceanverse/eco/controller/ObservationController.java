package com.oceanverse.eco.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.annotation.OperateLog;
import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.annotation.RequireRole;
import com.oceanverse.common.result.Result;
import com.oceanverse.eco.service.ObservationLocationService;
import com.oceanverse.eco.service.ObservationService;
import com.oceanverse.pojo.dto.ObservationQueryDTO;
import com.oceanverse.pojo.entity.Ecosystem;
import com.oceanverse.pojo.entity.Observation;
import com.oceanverse.pojo.entity.ObservationLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 观测管理接口
 */
@RestController
@RequestMapping("/api/observation")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService observationService;
    private final ObservationLocationService observationLocationService;

    @GetMapping("/list")
    public Result<Page<Observation>> list(ObservationQueryDTO query) {
        return Result.success(observationService.listObservations(query));
    }

    @GetMapping("/{id}")
    public Result<Observation> detail(@PathVariable Long id) {
        return Result.success(observationService.getDetail(id));
    }

    @RequireRole({"SUPER_ADMIN", "RESEARCHER", "OBSERVER"})
    @RequirePermission("observation:create")
    @OperateLog(module = "观测管理", type = OperateLog.OperateType.CREATE)
    @PostMapping
    public Result<Void> create(@RequestBody Observation observation) {
        observationService.create(observation);
        return Result.success();
    }

    @RequireRole({"SUPER_ADMIN", "RESEARCHER", "OBSERVER"})
    @RequirePermission("observation:update")
    @OperateLog(module = "观测管理", type = OperateLog.OperateType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Observation observation) {
        observation.setId(id);
        observationService.update(observation);
        return Result.success();
    }

    @RequireRole({"SUPER_ADMIN", "RESEARCHER", "OBSERVER"})
    @RequirePermission("observation:delete")
    @OperateLog(module = "观测管理", type = OperateLog.OperateType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        observationService.delete(id);
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(observationService.getStatistics());
    }

    @GetMapping("/locations")
    public Result<List<ObservationLocation>> locations() {
        return Result.success(observationLocationService.listAll());
    }

    @GetMapping("/ecosystems")
    public Result<List<Ecosystem>> ecosystems() {
        return Result.success(observationService.listEcosystems());
    }

    @GetMapping("/map")
    public Result<List<Observation>> mapData() {
        return Result.success(observationService.getMapData());
    }
}
