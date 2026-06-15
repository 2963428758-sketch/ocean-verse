package com.oceanverse.species.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.SpeciesQueryDTO;
import com.oceanverse.pojo.entity.Species;
import com.oceanverse.species.service.SpeciesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 海洋物种管理接口 — 成员B
 */
@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;

    @GetMapping("/list")
    public Result<Page<Species>> list(SpeciesQueryDTO query) {
        return Result.success(speciesService.listSpecies(query));
    }

    @GetMapping("/{id}")
    public Result<Species> detail(@PathVariable Long id) {
        return Result.success(speciesService.getSpeciesDetail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Species species) {
        speciesService.createSpecies(species);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Species species) {
        species.setId(id);
        speciesService.updateSpecies(species);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        speciesService.deleteSpecies(id);
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<Object> statistics() {
        return Result.success(speciesService.getStatistics());
    }
}
