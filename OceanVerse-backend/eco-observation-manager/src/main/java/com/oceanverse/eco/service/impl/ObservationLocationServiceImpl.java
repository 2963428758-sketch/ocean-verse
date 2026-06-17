package com.oceanverse.eco.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oceanverse.eco.mapper.ObservationLocationMapper;
import com.oceanverse.eco.service.ObservationLocationService;
import com.oceanverse.pojo.entity.ObservationLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationLocationServiceImpl implements ObservationLocationService {

    private final ObservationLocationMapper observationLocationMapper;

    @Override
    public List<ObservationLocation> listAll() {
        return observationLocationMapper.selectList(
                new LambdaQueryWrapper<ObservationLocation>()
                        .orderByAsc(ObservationLocation::getLocationCode)
        );
    }
}
