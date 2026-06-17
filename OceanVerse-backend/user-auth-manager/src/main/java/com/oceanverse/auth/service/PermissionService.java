package com.oceanverse.auth.service;

import com.oceanverse.pojo.vo.PermissionTreeVO;

import java.util.List;

public interface PermissionService {

    /**
     * 获取全量权限树
     */
    List<PermissionTreeVO> getPermissionTree();

    /**
     * 获取用户的权限码列表
     */
    List<String> getUserPermCodes(Long userId);

    /**
     * 获取角色的权限ID列表
     */
    List<Long> getRolePermIds(Long roleId);
}
