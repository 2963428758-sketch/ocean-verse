package com.oceanverse.species.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.pojo.dto.SpeciesQueryDTO;
import com.oceanverse.pojo.entity.Species;
import com.oceanverse.species.mapper.SpeciesMapper;
import com.oceanverse.species.service.SpeciesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeciesServiceImpl implements SpeciesService {

    private final SpeciesMapper speciesMapper;

    @Override
    public Page<Species> listSpecies(SpeciesQueryDTO query) {
        Page<Species> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Species> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w
                    .like(Species::getChineseName, query.getKeyword())
                    .or().like(Species::getCommonName, query.getKeyword())
                    .or().like(Species::getScientificName, query.getKeyword())
            );
        }
        if (StringUtils.hasText(query.getFamily())) {
            wrapper.eq(Species::getFamily, query.getFamily());
        }
        if (StringUtils.hasText(query.getIucnStatus())) {
            wrapper.eq(Species::getIucnStatus, query.getIucnStatus());
        }

        wrapper.orderByDesc(Species::getCreateTime);
        return speciesMapper.selectPage(page, wrapper);
    }

    @Override
    public Species getSpeciesDetail(Long id) {
        Species species = speciesMapper.selectById(id);
        if (species == null) {
            throw BusinessException.notFound("物种");
        }
        return species;
    }

    @Override
    public void createSpecies(Species species) {
        species.setCreateTime(LocalDateTime.now());
        species.setUpdateTime(LocalDateTime.now());
        speciesMapper.insert(species);
        log.info("新增物种: {}", species.getChineseName());
    }

    @Override
    public void updateSpecies(Species species) {
        species.setUpdateTime(LocalDateTime.now());
        speciesMapper.updateById(species);
    }

    @Override
    public void deleteSpecies(Long id) {
        speciesMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", speciesMapper.selectCount(null));
        // TODO: 按保护等级统计、按科统计等
        return stats;
    }
}
