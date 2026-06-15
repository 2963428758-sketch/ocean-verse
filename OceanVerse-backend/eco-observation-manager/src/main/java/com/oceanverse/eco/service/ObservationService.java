package com.oceanverse.eco.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.pojo.entity.Observation;

public interface ObservationService {
    Page<Observation> listObservations(Integer page, Integer size);
    Observation getDetail(Long id);
    void create(Observation observation);
    void update(Observation observation);
    void delete(Long id);
}
