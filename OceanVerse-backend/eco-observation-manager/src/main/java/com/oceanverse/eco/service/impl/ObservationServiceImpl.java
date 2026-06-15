package com.oceanverse.eco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.eco.mapper.ObservationMapper;
import com.oceanverse.eco.service.ObservationService;
import com.oceanverse.pojo.entity.Observation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationServiceImpl implements ObservationService {

    private final ObservationMapper observationMapper;

    @Override
    public Page<Observation> listObservations(Integer page, Integer size) {
        return observationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Observation>()
                        .orderByDesc(Observation::getCreateTime)
        );
    }

    @Override
    public Observation getDetail(Long id) {
        Observation obs = observationMapper.selectById(id);
        if (obs == null) throw BusinessException.notFound("观测记录");
        return obs;
    }

    @Override
    public void create(Observation observation) {
        observation.setCreateTime(LocalDateTime.now());
        observation.setUpdateTime(LocalDateTime.now());
        observationMapper.insert(observation);
    }

    @Override
    public void update(Observation observation) {
        observation.setUpdateTime(LocalDateTime.now());
        observationMapper.updateById(observation);
    }

    @Override
    public void delete(Long id) {
        observationMapper.deleteById(id);
    }
}
