package com.oceanverse.visual.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oceanverse.pojo.entity.Species;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

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

    @Select("SELECT COUNT(*) FROM community_post WHERE deleted = 0")
    long countPosts();

    @Select("SELECT family, COUNT(*) as count FROM species WHERE deleted = 0 AND family IS NOT NULL GROUP BY family ORDER BY count DESC")
    List<Map<String, Object>> countSpeciesByFamily();

    @Select("SELECT iucn_status, COUNT(*) as count FROM species WHERE deleted = 0 AND iucn_status IS NOT NULL GROUP BY iucn_status ORDER BY count DESC")
    List<Map<String, Object>> countSpeciesByIucn();

    @Select("SELECT sd.id, sd.species_id, s.chinese_name, s.scientific_name, s.family, s.iucn_status, s.description, " +
            "sd.region_name, sd.country, sd.province, sd.latitude, sd.longitude, sd.distribution_type, sd.habitat_type, " +
            "sm.file_url as image_url " +
            "FROM species_distribution sd " +
            "LEFT JOIN species s ON sd.species_id = s.id AND s.deleted = 0 " +
            "LEFT JOIN species_media sm ON sm.species_id = s.id AND sm.is_primary = 1 AND sm.deleted = 0 " +
            "WHERE sd.deleted = 0 AND s.id IS NOT NULL")
    List<Map<String, Object>> getSpeciesDistributionAll();

    @Select("SELECT sd.id, sd.species_id, s.chinese_name, s.scientific_name, s.family, s.iucn_status, s.description, " +
            "sd.region_name, sd.country, sd.province, sd.latitude, sd.longitude, sd.distribution_type, sd.habitat_type, " +
            "sm.file_url as image_url " +
            "FROM species_distribution sd " +
            "LEFT JOIN species s ON sd.species_id = s.id AND s.deleted = 0 " +
            "LEFT JOIN species_media sm ON sm.species_id = s.id AND sm.is_primary = 1 AND sm.deleted = 0 " +
            "WHERE sd.deleted = 0 AND s.id IS NOT NULL AND sd.species_id = #{speciesId}")
    List<Map<String, Object>> getSpeciesDistributionBySpeciesId(@Param("speciesId") Long speciesId);

    @Select("SELECT DATE_FORMAT(observation_date, '%Y-%m') as period, COUNT(*) as count " +
            "FROM observation WHERE deleted = 0 GROUP BY period ORDER BY period")
    List<Map<String, Object>> getObservationTrendMonthly();

    @Select("SELECT DATE_FORMAT(observation_date, '%x-W%v') as period, COUNT(*) as count " +
            "FROM observation WHERE deleted = 0 GROUP BY period ORDER BY period")
    List<Map<String, Object>> getObservationTrendWeekly();

    @Select("SELECT DATE_FORMAT(observation_date, '%Y-%m-%d') as period, COUNT(*) as count " +
            "FROM observation WHERE deleted = 0 GROUP BY period ORDER BY period")
    List<Map<String, Object>> getObservationTrendDaily();
}
