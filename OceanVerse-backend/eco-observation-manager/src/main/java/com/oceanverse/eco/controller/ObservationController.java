package com.oceanverse.eco.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.entity.Observation;
import com.oceanverse.eco.service.ObservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 观测管理接口
 * <p>
 * 列表和详情接口需要登录；增删改需要 observation 相关权限。
 * Swagger Tag: Observation（生态观测）
 */
@Tag(name = "Observation", description = "生态观测管理接口")
@RestController
@RequestMapping("/api/observation")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService observationService;

    @Operation(summary = "观测记录列表", description = "分页查询观测记录")
    @GetMapping("/list")
    public Result<Page<Observation>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(observationService.listObservations(page, size));
    }

    @Operation(summary = "观测详情", description = "获取单条观测记录详情")
    @GetMapping("/{id}")
    public Result<Observation> detail(
            @Parameter(description = "观测记录ID") @PathVariable Long id) {
        return Result.success(observationService.getDetail(id));
    }

    @Operation(summary = "创建观测记录", description = "新增生态观测记录（需要 observation:create 权限）")
    @PostMapping
    @RequirePermission("observation:create")
    public Result<Void> create(@RequestBody Observation observation) {
        observationService.create(observation);
        return Result.success();
    }

    @Operation(summary = "更新观测记录", description = "修改观测记录（需要 observation:update 权限）")
    @PutMapping("/{id}")
    @RequirePermission("observation:update")
    public Result<Void> update(
            @Parameter(description = "观测记录ID") @PathVariable Long id,
            @RequestBody Observation observation) {
        observation.setId(id);
        observationService.update(observation);
        return Result.success();
    }

    @Operation(summary = "删除观测记录", description = "删除观测记录（需要 observation:delete 权限）")
    @DeleteMapping("/{id}")
    @RequirePermission("observation:delete")
    public Result<Void> delete(
            @Parameter(description = "观测记录ID") @PathVariable Long id) {
        observationService.delete(id);
        return Result.success();
    }
}
