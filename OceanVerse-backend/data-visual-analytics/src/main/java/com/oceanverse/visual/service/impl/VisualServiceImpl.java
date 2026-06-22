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
        data.put("totalPosts", visualMapper.countPosts());
        return data;
    }

    @Override
    public List<Map<String, Object>> getSpeciesDistribution(Long speciesId) {
        if (speciesId != null) {
            return visualMapper.getSpeciesDistributionBySpeciesId(speciesId);
        }
        return visualMapper.getSpeciesDistributionAll();
    }

    @Override
    public List<Map<String, Object>> getObservationTrend(String period) {
        return switch (period) {
            case "weekly" -> visualMapper.getObservationTrendWeekly();
            case "daily" -> visualMapper.getObservationTrendDaily();
            default -> visualMapper.getObservationTrendMonthly();
        };
    }

    @Override
    public List<Map<String, Object>> getSpeciesByFamily() {
        return visualMapper.countSpeciesByFamily();
    }

    @Override
    public List<Map<String, Object>> getSpeciesByIucn() {
        return visualMapper.countSpeciesByIucn();
    }
}
