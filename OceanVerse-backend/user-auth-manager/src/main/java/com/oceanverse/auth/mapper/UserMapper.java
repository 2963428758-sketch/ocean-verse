package com.oceanverse.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oceanverse.pojo.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 查询用户首个角色标识（兼容旧逻辑）
     */
    @Select("SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0 " +
            "LIMIT 1")
    String selectRoleCodeByUserId(@Param("userId") Long userId);

    /**
     * 查询用户所有角色标识列表
     */
    @Select("SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<String> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 批量查询多个用户的角色（解决 N+1 问题）
     * 返回 Map: user_id → role_code（取首个角色）
     */
    @Select("<script>" +
            "SELECT ur.user_id, r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id IN " +
            "<foreach item='id' collection='userIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND r.deleted = 0" +
            "</script>")
    List<Map<String, Object>> selectRoleMapByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 为用户插入默认角色
     */
    @Insert("INSERT INTO sys_user_role (user_id, role_id) " +
            "SELECT #{userId}, r.id FROM sys_role r WHERE r.role_code = #{roleCode} AND r.deleted = 0")
    int insertUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
