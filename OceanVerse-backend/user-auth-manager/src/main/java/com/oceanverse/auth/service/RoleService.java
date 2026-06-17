package com.oceanverse.auth.service;

import com.oceanverse.common.result.PageResult;
import com.oceanverse.pojo.dto.AssignPermissionsDTO;
import com.oceanverse.pojo.dto.RoleCreateDTO;
import com.oceanverse.pojo.dto.RoleUpdateDTO;
import com.oceanverse.pojo.vo.RoleVO;

public interface RoleService {

    /**
     * 角色分页列表
     */
    PageResult<RoleVO> listRoles(int page, int size, String keyword);

    /**
     * 角色详情
     */
    RoleVO getRole(Long roleId);

    /**
     * 创建角色
     */
    void createRole(RoleCreateDTO dto);

    /**
     * 更新角色
     */
    void updateRole(Long roleId, RoleUpdateDTO dto);

    /**
     * 删除角色（逻辑删除）
     */
    void deleteRole(Long roleId);

    /**
     * 启用/禁用角色
     */
    void toggleStatus(Long roleId, int status);

    /**
     * 为角色分配权限（先删后插）
     */
    void assignPermissions(AssignPermissionsDTO dto);
}
