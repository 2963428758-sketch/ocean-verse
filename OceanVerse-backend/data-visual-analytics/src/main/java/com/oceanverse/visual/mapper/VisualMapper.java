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

    // ==================== 导出用查询 ====================

    /**
     * 查询全部观测记录（导出用）
     */
    @Select("SELECT id, observation_code, observation_type, observation_date, observation_time, " +
            "duration_minutes, latitude, longitude, depth, water_temperature, salinity, ph, " +
            "weather_condition, sea_condition, observer_name, organization, equipment_used, notes " +
            "FROM observation WHERE deleted = 0 ORDER BY observation_date DESC")
    List<Map<String, Object>> getAllObservationsForExport();

    /**
     * 按观测类型筛选观测记录（导出用）
     */
    @Select("<script>" +
            "SELECT id, observation_code, observation_type, observation_date, observation_time, " +
            "duration_minutes, latitude, longitude, depth, water_temperature, salinity, ph, " +
            "weather_condition, sea_condition, observer_name, organization, equipment_used, notes " +
            "FROM observation WHERE deleted = 0 " +
            "<if test='types != null and types.size() > 0'>" +
            "AND observation_type IN " +
            "<foreach collection='types' item='t' open='(' separator=',' close=')'>#{t}</foreach> " +
            "</if>" +
            "<if test='startDate != null and startDate != \"\"'>" +
            "AND observation_date &gt;= #{startDate} " +
            "</if>" +
            "<if test='endDate != null and endDate != \"\"'>" +
            "AND observation_date &lt;= #{endDate} " +
            "</if>" +
            "ORDER BY observation_date DESC" +
            "</script>")
    List<Map<String, Object>> getObservationsForExport(@Param("types") List<String> types,
                                                        @Param("startDate") String startDate,
                                                        @Param("endDate") String endDate);

    /**
     * 按 IUCN 等级筛选物种分布（导出用）
     */
    @Select("<script>" +
            "SELECT sd.id, sd.species_id, s.chinese_name, s.scientific_name, s.family, s.iucn_status, s.description, " +
            "sd.region_name, sd.country, sd.province, sd.latitude, sd.longitude, sd.distribution_type, sd.habitat_type " +
            "FROM species_distribution sd " +
            "LEFT JOIN species s ON sd.species_id = s.id AND s.deleted = 0 " +
            "WHERE sd.deleted = 0 AND s.id IS NOT NULL " +
            "<if test='iucnList != null and iucnList.size() > 0'>" +
            "AND s.iucn_status IN " +
            "<foreach collection='iucnList' item='i' open='(' separator=',' close=')'>#{i}</foreach> " +
            "</if>" +
            "<if test='familyList != null and familyList.size() > 0'>" +
            "AND s.family IN " +
            "<foreach collection='familyList' item='f' open='(' separator=',' close=')'>#{f}</foreach> " +
            "</if>" +
            "ORDER BY s.chinese_name" +
            "</script>")
    List<Map<String, Object>> getSpeciesDistributionForExport(@Param("iucnList") List<String> iucnList,
                                                               @Param("familyList") List<String> familyList);
}
