package com.oceanverse.visual.service.impl;

import com.oceanverse.visual.mapper.VisualMapper;
import com.oceanverse.visual.service.VisualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisualServiceImpl implements VisualService {

    private final VisualMapper visualMapper;

    @Override
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalSpecies", visualMapper.countSpecies());
        data.put("totalObservations", visualMapper.countObservations());
        data.put("totalUsers", visualMapper.countUsers());
        data.put("totalRecognitions", visualMapper.countRecognitions());
        return data;
    }

    @Override
    public List<Map<String, Object>> getSpeciesDistribution(Long speciesId) {
        // TODO: 查询 species_distribution 表
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getObservationTrend(String period) {
        // TODO: 按时间分组查询 observation 表
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getSpeciesByFamily() {
        // TODO: GROUP BY family 统计
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getSpeciesByIucn() {
        // TODO: GROUP BY iucn_status 统计
        return new ArrayList<>();
    }
}
