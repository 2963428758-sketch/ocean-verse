package com.oceanverse.species.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.pojo.dto.SpeciesQueryDTO;
import com.oceanverse.pojo.entity.Species;

import java.util.Map;

public interface SpeciesService {
    Page<Species> listSpecies(SpeciesQueryDTO query);
    Species getSpeciesDetail(Long id);
    void createSpecies(Species species);
    void updateSpecies(Species species);
    void deleteSpecies(Long id);
    Map<String, Object> getStatistics();
}
