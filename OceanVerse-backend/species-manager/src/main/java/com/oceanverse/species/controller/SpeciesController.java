package com.oceanverse.species.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.annotation.OperateLog;
import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.annotation.RequireRole;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.SpeciesQueryDTO;
import com.oceanverse.pojo.entity.Species;
import com.oceanverse.pojo.entity.SpeciesDistribution;
import com.oceanverse.pojo.entity.SpeciesMedia;
import com.oceanverse.species.service.SpeciesMediaService;
import com.oceanverse.species.service.SpeciesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 海洋物种管理接口 — 成员B
 */
@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;
    private final SpeciesMediaService speciesMediaService;

    @GetMapping("/list")
    public Result<Page<Species>> list(SpeciesQueryDTO query) {
        return Result.success(speciesService.listSpecies(query));
    }

    @GetMapping("/{id}")
    public Result<Species> detail(@PathVariable Long id) {
        return Result.success(speciesService.getSpeciesDetail(id));
    }

    @RequireRole({"SUPER_ADMIN", "RESEARCHER"})
    @RequirePermission("species:create")
    @OperateLog(module = "物种管理", type = OperateLog.OperateType.CREATE)
    @PostMapping
    public Result<Long> create(@RequestBody Species species) {
        speciesService.createSpecies(species);
        return Result.success(species.getId());
    }

    @RequireRole({"SUPER_ADMIN", "RESEARCHER"})
    @RequirePermission("species:update")
    @OperateLog(module = "物种管理", type = OperateLog.OperateType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Species species) {
        species.setId(id);
        speciesService.updateSpecies(species);
        return Result.success();
    }

    @RequireRole({"SUPER_ADMIN", "RESEARCHER"})
    @RequirePermission("species:delete")
    @OperateLog(module = "物种管理", type = OperateLog.OperateType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        speciesService.deleteSpecies(id);
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(speciesService.getStatistics());
    }

    @GetMapping("/{id}/distributions")
    public Result<List<SpeciesDistribution>> distributions(@PathVariable Long id) {
        return Result.success(speciesService.getSpeciesDistributions(id));
    }

    // ==================== 物种媒体接口 ====================

    /**
     * 上传物种图片（支持批量上传）
     */
    @RequireRole({"SUPER_ADMIN", "RESEARCHER"})
    @RequirePermission("species:create")
    @PostMapping("/{id}/media")
    public Result<List<SpeciesMedia>> uploadMedia(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        return Result.success(speciesMediaService.uploadMedia(id, files));
    }

    /**
     * 获取物种的所有媒体资源
     */
    @GetMapping("/{id}/media")
    public Result<List<SpeciesMedia>> listMedia(@PathVariable Long id) {
        return Result.success(speciesMediaService.getMediaBySpeciesId(id));
    }

    /**
     * 删除媒体资源
     */
    @RequireRole({"SUPER_ADMIN", "RESEARCHER"})
    @RequirePermission("species:delete")
    @DeleteMapping("/media/{mediaId}")
    public Result<Void> deleteMedia(@PathVariable Long mediaId) {
        speciesMediaService.deleteMedia(mediaId);
        return Result.success();
    }

    /**
     * 设置主图
     */
    @RequireRole({"SUPER_ADMIN", "RESEARCHER"})
    @RequirePermission("species:update")
    @PutMapping("/media/{mediaId}/primary")
    public Result<Void> setPrimary(@PathVariable Long mediaId) {
        speciesMediaService.setPrimaryMedia(mediaId);
        return Result.success();
    }
}
