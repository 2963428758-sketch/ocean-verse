package com.oceanverse.species.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.annotation.RequireRole;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.SpeciesQueryDTO;
import com.oceanverse.pojo.entity.Species;
import com.oceanverse.species.service.SpeciesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 海洋物种管理接口 — 成员B
 * <p>
 * 公开接口（无需登录）：list、detail
 * 管理接口（需登录 + 权限）：create、update、delete、statistics
 * <p>
 * Swagger Tag: Species（物种管理）
 */
@Tag(name = "Species", description = "海洋物种管理接口")
@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;

    @Operation(summary = "物种分页列表", description = "按条件查询物种列表（公开接口）")
    @GetMapping("/list")
    public Result<Page<Species>> list(SpeciesQueryDTO query) {
        return Result.success(speciesService.listSpecies(query));
    }

    @Operation(summary = "物种详情", description = "根据ID获取物种详情（公开接口）")
    @GetMapping("/{id}")
    public Result<Species> detail(
            @Parameter(description = "物种ID") @PathVariable Long id) {
        return Result.success(speciesService.getSpeciesDetail(id));
    }

    @Operation(summary = "创建物种", description = "新增物种记录（需要 species:create 权限）")
    @PostMapping
    @RequirePermission("species:create")
    public Result<Void> create(@RequestBody Species species) {
        speciesService.createSpecies(species);
        return Result.success();
    }

    @Operation(summary = "更新物种", description = "修改物种信息（需要 species:update 权限）")
    @PutMapping("/{id}")
    @RequirePermission("species:update")
    public Result<Void> update(
            @Parameter(description = "物种ID") @PathVariable Long id,
            @RequestBody Species species) {
        species.setId(id);
        speciesService.updateSpecies(species);
        return Result.success();
    }

    @Operation(summary = "删除物种", description = "删除物种记录（需要 species:delete 权限）")
    @DeleteMapping("/{id}")
    @RequirePermission("species:delete")
    public Result<Void> delete(
            @Parameter(description = "物种ID") @PathVariable Long id) {
        speciesService.deleteSpecies(id);
        return Result.success();
    }

    @Operation(summary = "物种统计", description = "获取物种统计数据（需要登录）")
    @GetMapping("/statistics")
    @RequireRole({"SUPER_ADMIN", "ADMIN", "RESEARCHER"})
    public Result<Object> statistics() {
        return Result.success(speciesService.getStatistics());
    }
}
