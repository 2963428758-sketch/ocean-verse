package com.oceanverse.eco.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.entity.Observation;
import com.oceanverse.eco.service.ObservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 观测管理接口
 */
@RestController
@RequestMapping("/api/observation")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService observationService;

    @GetMapping("/list")
    public Result<Page<Observation>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(observationService.listObservations(page, size));
    }

    @GetMapping("/{id}")
    public Result<Observation> detail(@PathVariable Long id) {
        return Result.success(observationService.getDetail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Observation observation) {
        observationService.create(observation);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Observation observation) {
        observation.setId(id);
        observationService.update(observation);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        observationService.delete(id);
        return Result.success();
    }
}
