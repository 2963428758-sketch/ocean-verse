package com.oceanverse.species.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oceanverse.pojo.entity.Species;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SpeciesMapper extends BaseMapper<Species> {
}
