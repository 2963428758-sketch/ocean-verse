package com.oceanverse.visual.service.impl;

import com.oceanverse.visual.mapper.VisualMapper;
import com.oceanverse.visual.service.VisualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public List<Map<String, Object>> getSpeciesDistribution(Long speciesId, String iucnStatus, String family) {
        if (speciesId != null) {
            return visualMapper.getSpeciesDistributionBySpeciesId(speciesId);
        }
        List<String> iucnList = parseList(iucnStatus);
        List<String> familyList = parseList(family);
        if (iucnList != null || familyList != null) {
            return visualMapper.getSpeciesDistributionForExport(iucnList, familyList);
        }
        return visualMapper.getSpeciesDistributionAll();
    }

    private List<String> parseList(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
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
