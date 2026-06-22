package com.oceanverse.eco.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.pojo.dto.ObservationQueryDTO;
import com.oceanverse.pojo.entity.Ecosystem;
import com.oceanverse.pojo.entity.Observation;

import java.util.List;
import java.util.Map;

public interface ObservationService {
    Page<Observation> listObservations(ObservationQueryDTO query);
    Observation getDetail(Long id);
    void create(Observation observation);
    void update(Observation observation);
    void delete(Long id);
    Map<String, Object> getStatistics();
    List<Ecosystem> listEcosystems();
    List<Observation> getMapData();
}
