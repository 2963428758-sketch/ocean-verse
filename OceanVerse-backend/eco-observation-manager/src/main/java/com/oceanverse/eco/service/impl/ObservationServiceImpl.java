package com.oceanverse.eco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.eco.mapper.EcosystemMapper;
import com.oceanverse.eco.mapper.ObservationMapper;
import com.oceanverse.eco.service.ObservationService;
import com.oceanverse.pojo.dto.ObservationQueryDTO;
import com.oceanverse.pojo.entity.Ecosystem;
import com.oceanverse.pojo.entity.Observation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationServiceImpl implements ObservationService {

    private final ObservationMapper observationMapper;
    private final EcosystemMapper ecosystemMapper;

    @Override
    public Page<Observation> listObservations(ObservationQueryDTO query) {
        Page<Observation> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Observation> wrapper = new LambdaQueryWrapper<>();

        // 关键词：匹配编号或观测员名称
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w
                    .like(Observation::getObservationCode, query.getKeyword())
                    .or().like(Observation::getObserverName, query.getKeyword())
            );
        }
        if (StringUtils.hasText(query.getObservationType())) {
            wrapper.eq(Observation::getObservationType, query.getObservationType());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(Observation::getObservationDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(Observation::getObservationDate, query.getEndDate());
        }
        if (query.getEcosystemId() != null) {
            wrapper.eq(Observation::getEcosystemId, query.getEcosystemId());
        }
        if (query.getLocationId() != null) {
            wrapper.eq(Observation::getLocationId, query.getLocationId());
        }
        if (StringUtils.hasText(query.getObserverName())) {
            wrapper.like(Observation::getObserverName, query.getObserverName());
        }

        // 排序
        if ("observationCode".equals(query.getSort())) {
            wrapper.orderByAsc(Observation::getObservationCode);
        } else if ("observationDate".equals(query.getSort())) {
            wrapper.orderByDesc(Observation::getObservationDate);
        } else {
            wrapper.orderByDesc(Observation::getCreateTime);
        }
        return observationMapper.selectPage(page, wrapper);
    }

    @Override
    public Observation getDetail(Long id) {
        Observation obs = observationMapper.selectById(id);
        if (obs == null) throw BusinessException.notFound("观测记录");
        return obs;
    }

    @Override
    public void create(Observation observation) {
        // 自动生成编号
        if (!StringUtils.hasText(observation.getObservationCode())) {
            observation.setObservationCode(generateObservationCode());
        }
        // 校验编号唯一性
        LambdaQueryWrapper<Observation> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(Observation::getObservationCode, observation.getObservationCode());
        if (observationMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException("观测编号已存在: " + observation.getObservationCode());
        }
        observation.setCreateTime(LocalDateTime.now());
        observation.setUpdateTime(LocalDateTime.now());
        observationMapper.insert(observation);
        log.info("新增观测: {}", observation.getObservationCode());
    }

    @Override
    public void update(Observation observation) {
        Observation existing = observationMapper.selectById(observation.getId());
        if (existing == null) throw BusinessException.notFound("观测记录");
        // 校验编号唯一性（排除自身）
        if (StringUtils.hasText(observation.getObservationCode())) {
            LambdaQueryWrapper<Observation> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(Observation::getObservationCode, observation.getObservationCode())
                    .ne(Observation::getId, observation.getId());
            if (observationMapper.selectCount(codeWrapper) > 0) {
                throw new BusinessException("观测编号已存在: " + observation.getObservationCode());
            }
        }
        observation.setUpdateTime(LocalDateTime.now());
        observationMapper.updateById(observation);
        log.info("更新观测: id={}, code={}", observation.getId(), observation.getObservationCode());
    }

    @Override
    public void delete(Long id) {
        Observation existing = observationMapper.selectById(id);
        if (existing == null) throw BusinessException.notFound("观测记录");
        observationMapper.deleteById(id);
        log.info("删除观测: id={}, code={}", id, existing.getObservationCode());
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalCount = observationMapper.selectCount(null);
        stats.put("totalCount", totalCount);

        List<Observation> all = observationMapper.selectList(null);

        // 按观测类型分组
        Map<String, Long> byType = all.stream()
                .filter(o -> StringUtils.hasText(o.getObservationType()))
                .collect(Collectors.groupingBy(Observation::getObservationType, Collectors.counting()));
        stats.put("byType", byType);

        // 本月数量
        LocalDate now = LocalDate.now();
        long thisMonthCount = all.stream()
                .filter(o -> o.getObservationDate() != null
                        && o.getObservationDate().getYear() == now.getYear()
                        && o.getObservationDate().getMonth() == now.getMonth())
                .count();
        stats.put("thisMonthCount", thisMonthCount);

        // 本年数量
        long thisYearCount = all.stream()
                .filter(o -> o.getObservationDate() != null
                        && o.getObservationDate().getYear() == now.getYear())
                .count();
        stats.put("thisYearCount", thisYearCount);

        // 平均水温
        double avgTemp = all.stream()
                .filter(o -> o.getWaterTemperature() != null)
                .mapToDouble(o -> o.getWaterTemperature().doubleValue())
                .average().orElse(0.0);
        stats.put("avgWaterTemperature", BigDecimal.valueOf(avgTemp).setScale(2, RoundingMode.HALF_UP));

        // 平均盐度
        double avgSal = all.stream()
                .filter(o -> o.getSalinity() != null)
                .mapToDouble(o -> o.getSalinity().doubleValue())
                .average().orElse(0.0);
        stats.put("avgSalinity", BigDecimal.valueOf(avgSal).setScale(2, RoundingMode.HALF_UP));

        // 按生态系统分组
        List<Ecosystem> ecosystems = ecosystemMapper.selectList(null);
        Map<String, Long> byEcosystem = new HashMap<>();
        for (Ecosystem eco : ecosystems) {
            long count = all.stream()
                    .filter(o -> eco.getId().equals(o.getEcosystemId()))
                    .count();
            byEcosystem.put(eco.getEcosystemName(), count);
        }
        stats.put("byEcosystem", byEcosystem);

        return stats;
    }

    @Override
    public List<Ecosystem> listEcosystems() {
        return ecosystemMapper.selectList(
                new LambdaQueryWrapper<Ecosystem>().orderByAsc(Ecosystem::getEcosystemCode)
        );
    }

    @Override
    public List<Observation> getMapData() {
        LambdaQueryWrapper<Observation> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Observation::getLatitude)
               .isNotNull(Observation::getLongitude)
               .orderByDesc(Observation::getObservationDate);
        return observationMapper.selectList(wrapper);
    }

    private String generateObservationCode() {
        LambdaQueryWrapper<Observation> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Observation::getId).last("LIMIT 1");
        Observation latest = observationMapper.selectOne(wrapper);
        if (latest == null) return "OBS001";
        String lastCode = latest.getObservationCode();
        if (lastCode == null) return "OBS001";
        // 用正则提取末尾数字，兼容各种编号格式
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)$").matcher(lastCode);
        int num = 1;
        if (m.find()) {
            num = Integer.parseInt(m.group(1)) + 1;
        }
        return String.format("OBS%03d", num);
    }
}
