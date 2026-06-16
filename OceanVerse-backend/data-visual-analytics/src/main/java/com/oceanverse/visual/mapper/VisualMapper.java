package com.oceanverse.visual.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oceanverse.pojo.entity.Species;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VisualMapper extends BaseMapper<Species> {

    @Select("SELECT COUNT(*) FROM species WHERE deleted = 0")
    long countSpecies();

    @Select("SELECT COUNT(*) FROM observation WHERE deleted = 0")
    long countObservations();

    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    long countUsers();

    @Select("SELECT COUNT(*) FROM image_recognition WHERE deleted = 0")
    long countRecognitions();
}
