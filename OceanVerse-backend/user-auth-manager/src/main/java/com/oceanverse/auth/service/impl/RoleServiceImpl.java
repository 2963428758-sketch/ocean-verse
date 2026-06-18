package com.oceanverse.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.auth.mapper.*;
import com.oceanverse.auth.service.RoleService;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.common.result.PageResult;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.pojo.dto.AssignPermissionsDTO;
import com.oceanverse.pojo.dto.RoleCreateDTO;
import com.oceanverse.pojo.dto.RoleUpdateDTO;
import com.oceanverse.pojo.entity.SysPermission;
import com.oceanverse.pojo.entity.SysRole;
import com.oceanverse.pojo.entity.SysRolePermission;
import com.oceanverse.pojo.entity.SysUserRole;
import com.oceanverse.pojo.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;

    @Override
    public PageResult<RoleVO> listRoles(int page, int size, String keyword) {
        Page<SysRole> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SysRole::getRoleName, keyword)
                    .or().like(SysRole::getRoleCode, keyword);
        }
        wrapper.orderByAsc(SysRole::getId);
        Page<SysRole> result = roleMapper.selectPage(pageParam, wrapper);

        List<SysRole> records = result.getRecords();

        // 批量查询所有角色的权限ID和权限名（解决 N+1）
        List<Long> roleIds = records.stream().map(SysRole::getId).toList();
        Map<Long, List<Long>> permIdsMap = new HashMap<>();
        Map<Long, List<String>> permNamesMap = new HashMap<>();

        if (!roleIds.isEmpty()) {
            List<Map<String, Object>> permIdRows = permissionMapper.selectPermIdsByRoleIds(roleIds);
            for (Map<String, Object> row : permIdRows) {
                Long roleId = ((Number) row.get("role_id")).longValue();
                Long permId = ((Number) row.get("permission_id")).longValue();
                permIdsMap.computeIfAbsent(roleId, k -> new ArrayList<>()).add(permId);
            }

            List<Map<String, Object>> permNameRows = permissionMapper.selectPermNamesByRoleIds(roleIds);
            for (Map<String, Object> row : permNameRows) {
                Long roleId = ((Number) row.get("role_id")).longValue();
                String name = (String) row.get("name");
                permNamesMap.computeIfAbsent(roleId, k -> new ArrayList<>()).add(name);
            }
        }

        List<RoleVO> voList = records.stream()
                .map(role -> toRoleVO(role,
                        permIdsMap.getOrDefault(role.getId(), List.of()),
                        permNamesMap.getOrDefault(role.getId(), List.of())))
                .toList();

        return PageResult.of(voList, result.getTotal(), page, size);
    }

    @Override
    public RoleVO getRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色");
        }
        List<Long> permIds = permissionMapper.selectPermIdsByRoleId(roleId);
        List<String> permNames = permissionMapper.selectByRoleId(roleId)
                .stream()
                .map(SysPermission::getName)
                .toList();
        return toRoleVO(role, permIds, permNames);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleCreateDTO dto) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, dto.getRoleCode())
        );
        if (count > 0) {
            throw new BusinessException("角色代码已存在");
        }

        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setStatus(CommonConstants.USER_STATUS_NORMAL);
        role.setCreateTime(LocalDateTime.now());
        roleMapper.insert(role);

        log.info("创建角色: code={}, name={}", dto.getRoleCode(), dto.getRoleName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long roleId, RoleUpdateDTO dto) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色");
        }

        role.setRoleName(dto.getRoleName());
        if (dto.getDescription() != null) {
            role.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);

        log.info("更新角色: id={}, name={}", roleId, dto.getRoleName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色");
        }

        // 检查是否有用户关联
        Long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId)
        );
        if (userCount > 0) {
            throw new BusinessException("该角色下还有用户，无法删除");
        }

        // 删除角色权限关联
        rolePermissionMapper.deleteByRoleId(roleId);

        // 逻辑删除角色
        roleMapper.deleteById(roleId);

        log.info("删除角色: id={}, code={}", roleId, role.getRoleCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long roleId, int status) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色");
        }

        role.setStatus(status);
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);

        // 如果禁用角色，清除该角色下所有用户的权限缓存
        if (status != CommonConstants.USER_STATUS_NORMAL) {
            clearRoleUsersCache(roleId);
        }

        log.info("角色状态变更: id={}, status={}", roleId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(AssignPermissionsDTO dto) {
        SysRole role = roleMapper.selectById(dto.getRoleId());
        if (role == null) {
            throw BusinessException.notFound("角色");
        }

        // 先删后插（全量替换）
        rolePermissionMapper.deleteByRoleId(dto.getRoleId());

        for (Long permId : dto.getPermissionIds()) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(dto.getRoleId());
            rp.setPermissionId(permId);
            rolePermissionMapper.insert(rp);
        }

        // 清除该角色下所有用户的权限缓存
        clearRoleUsersCache(dto.getRoleId());

        log.info("角色分配权限: roleId={}, permCount={}", dto.getRoleId(), dto.getPermissionIds().size());
    }

    /**
     * 清除角色下所有用户的权限缓存
     */
    private void clearRoleUsersCache(Long roleId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId)
        );
        for (SysUserRole ur : userRoles) {
            redisUtil.delete(CommonConstants.REDIS_USER_PERMS + ur.getUserId());
            redisUtil.delete(CommonConstants.REDIS_USER_ROLES + ur.getUserId());
        }
    }

    private RoleVO toRoleVO(SysRole role, List<Long> permIds, List<String> permNames) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());
        vo.setPermissionIds(permIds);
        vo.setPermissionNames(permNames);
        return vo;
    }
}
