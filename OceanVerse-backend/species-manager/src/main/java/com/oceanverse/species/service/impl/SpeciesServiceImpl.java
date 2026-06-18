package com.oceanverse.species.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.pojo.dto.SpeciesQueryDTO;
import com.oceanverse.pojo.entity.Species;
import com.oceanverse.pojo.entity.SpeciesDistribution;
import com.oceanverse.species.mapper.SpeciesMapper;
import com.oceanverse.species.mapper.SpeciesDistributionMapper;
import com.oceanverse.species.service.SpeciesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeciesServiceImpl implements SpeciesService {

    private final SpeciesMapper speciesMapper;
    private final SpeciesDistributionMapper speciesDistributionMapper;

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
        if (StringUtils.hasText(query.getProtectionLevel())) {
            wrapper.eq(Species::getProtectionLevel, query.getProtectionLevel());
        }

        // 排序：默认按创建时间倒序，也可按编号升序
        if ("speciesCode".equals(query.getSort())) {
            wrapper.orderByAsc(Species::getSpeciesCode);
        } else {
            wrapper.orderByDesc(Species::getCreateTime);
        }
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
        // 校验物种编码唯一性
        LambdaQueryWrapper<Species> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(Species::getSpeciesCode, species.getSpeciesCode());
        if (speciesMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException("物种编码已存在: " + species.getSpeciesCode());
        }
        // 校验学名唯一性
        LambdaQueryWrapper<Species> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(Species::getScientificName, species.getScientificName());
        if (speciesMapper.selectCount(nameWrapper) > 0) {
            throw new BusinessException("物种学名已存在: " + species.getScientificName());
        }

        species.setCreateTime(LocalDateTime.now());
        species.setUpdateTime(LocalDateTime.now());
        speciesMapper.insert(species);
        log.info("新增物种: {}", species.getChineseName());

        // 如果前端传入了经纬度，自动创建初始分布记录
        if (species.getLongitude() != null && species.getLatitude() != null) {
            SpeciesDistribution distribution = new SpeciesDistribution();
            distribution.setSpeciesId(species.getId());
            distribution.setDistributionType("NATIVE");
            distribution.setLongitude(species.getLongitude());
            distribution.setLatitude(species.getLatitude());
            distribution.setIsPrimary(1);
            distribution.setDistributionStatus("COMMON");
            distribution.setCreateTime(LocalDateTime.now());
            distribution.setUpdateTime(LocalDateTime.now());
            speciesDistributionMapper.insert(distribution);
            log.info("自动创建物种分布记录: speciesId={}, 经度={}, 纬度={}",
                    species.getId(), species.getLongitude(), species.getLatitude());
        }
    }

    @Override
    public void updateSpecies(Species species) {
        // 校验物种是否存在
        Species existing = speciesMapper.selectById(species.getId());
        if (existing == null) {
            throw BusinessException.notFound("物种");
        }
        // 校验物种编码唯一性（排除自身）
        if (StringUtils.hasText(species.getSpeciesCode())) {
            LambdaQueryWrapper<Species> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(Species::getSpeciesCode, species.getSpeciesCode())
                    .ne(Species::getId, species.getId());
            if (speciesMapper.selectCount(codeWrapper) > 0) {
                throw new BusinessException("物种编码已存在: " + species.getSpeciesCode());
            }
        }
        // 校验学名唯一性（排除自身）
        if (StringUtils.hasText(species.getScientificName())) {
            LambdaQueryWrapper<Species> nameWrapper = new LambdaQueryWrapper<>();
            nameWrapper.eq(Species::getScientificName, species.getScientificName())
                    .ne(Species::getId, species.getId());
            if (speciesMapper.selectCount(nameWrapper) > 0) {
                throw new BusinessException("物种学名已存在: " + species.getScientificName());
            }
        }

        species.setUpdateTime(LocalDateTime.now());
        speciesMapper.updateById(species);
        log.info("更新物种: id={}, chineseName={}", species.getId(), species.getChineseName());
    }

    @Override
    public void deleteSpecies(Long id) {
        Species existing = speciesMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("物种");
        }
        speciesMapper.deleteById(id);
        log.info("删除物种: id={}, chineseName={}", id, existing.getChineseName());
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 物种总量
        long totalCount = speciesMapper.selectCount(null);
        stats.put("totalCount", totalCount);

        // 按保护等级统计（数据库 GROUP BY）
        List<Map<String, Object>> protectionLevelResult = speciesMapper.selectMaps(
                new LambdaQueryWrapper<Species>()
                        .select(Species::getProtectionLevel)
                        .isNotNull(Species::getProtectionLevel)
                        .ne(Species::getProtectionLevel, "")
                        .groupBy(Species::getProtectionLevel));
        Map<String, Long> byProtectionLevel = protectionLevelResult.stream()
                .collect(Collectors.toMap(
                        m -> String.valueOf(m.get("protection_level")),
                        m -> ((Number) m.get("count(*)")).longValue(),
                        (a, b) -> a));
        stats.put("byProtectionLevel", byProtectionLevel);

        // 按IUCN等级统计（数据库 GROUP BY）
        List<Map<String, Object>> iucnResult = speciesMapper.selectMaps(
                new LambdaQueryWrapper<Species>()
                        .select(Species::getIucnStatus)
                        .isNotNull(Species::getIucnStatus)
                        .ne(Species::getIucnStatus, "")
                        .groupBy(Species::getIucnStatus));
        Map<String, Long> byIucnStatus = iucnResult.stream()
                .collect(Collectors.toMap(
                        m -> String.valueOf(m.get("iucn_status")),
                        m -> ((Number) m.get("count(*)")).longValue(),
                        (a, b) -> a));
        stats.put("byIucnStatus", byIucnStatus);

        // 按科统计（数据库 GROUP BY）
        List<Map<String, Object>> familyResult = speciesMapper.selectMaps(
                new LambdaQueryWrapper<Species>()
                        .select(Species::getFamily)
                        .isNotNull(Species::getFamily)
                        .ne(Species::getFamily, "")
                        .groupBy(Species::getFamily));
        Map<String, Long> byFamily = familyResult.stream()
                .collect(Collectors.toMap(
                        m -> String.valueOf(m.get("family")),
                        m -> ((Number) m.get("count(*)")).longValue(),
                        (a, b) -> a));
        stats.put("byFamily", byFamily);

        // 特有种数量
        long endemicCount = speciesMapper.selectCount(
                new LambdaQueryWrapper<Species>()
                        .eq(Species::getIsEndemic, 1));
        stats.put("endemicCount", endemicCount);

        // 入侵物种数量
        long invasiveCount = speciesMapper.selectCount(
                new LambdaQueryWrapper<Species>()
                        .eq(Species::getIsInvasive, 1));
        stats.put("invasiveCount", invasiveCount);

        return stats;
    }

    @Override
    public List<SpeciesDistribution> getSpeciesDistributions(Long speciesId) {
        Species species = speciesMapper.selectById(speciesId);
        if (species == null) {
            throw BusinessException.notFound("物种");
        }
        LambdaQueryWrapper<SpeciesDistribution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpeciesDistribution::getSpeciesId, speciesId)
                .orderByDesc(SpeciesDistribution::getIsPrimary);
        return speciesDistributionMapper.selectList(wrapper);
    }
}
