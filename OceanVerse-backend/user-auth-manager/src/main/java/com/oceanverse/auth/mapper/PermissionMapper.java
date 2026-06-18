package com.oceanverse.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oceanverse.pojo.entity.SysPermission;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface PermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据用户ID查询全量权限码列表（通过角色关联）
     */
    @Select("SELECT DISTINCT p.perm_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限列表
     */
    @Select("SELECT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} " +
            "ORDER BY p.sort ASC")
    List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID查询权限ID列表
     */
    @Select("SELECT rp.permission_id FROM sys_role_permission rp " +
            "WHERE rp.role_id = #{roleId}")
    List<Long> selectPermIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量查询多个角色的权限ID映射（解决 N+1 问题）
     */
    @Select("<script>" +
            "SELECT rp.role_id, rp.permission_id FROM sys_role_permission rp " +
            "WHERE rp.role_id IN " +
            "<foreach item='id' collection='roleIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> selectPermIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 批量查询多个角色的权限名称映射
     */
    @Select("<script>" +
            "SELECT rp.role_id, p.name FROM sys_role_permission rp " +
            "INNER JOIN sys_permission p ON rp.permission_id = p.id " +
            "WHERE rp.role_id IN " +
            "<foreach item='id' collection='roleIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY p.sort ASC" +
            "</script>")
    List<Map<String, Object>> selectPermNamesByRoleIds(@Param("roleIds") List<Long> roleIds);
}
