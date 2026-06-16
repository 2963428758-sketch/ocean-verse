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
import com.oceanverse.pojo.entity.SysRole;
import com.oceanverse.pojo.entity.SysRolePermission;
import com.oceanverse.pojo.entity.SysUserRole;
import com.oceanverse.pojo.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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

        List<RoleVO> voList = result.getRecords().stream()
                .map(this::toRoleVO)
                .toList();

        return PageResult.of(voList, result.getTotal(), page, size);
    }

    @Override
    public RoleVO getRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色");
        }
        return toRoleVO(role);
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

    private RoleVO toRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());

        // 查询角色关联的权限
        List<Long> permIds = permissionMapper.selectPermIdsByRoleId(role.getId());
        vo.setPermissionIds(permIds);

        List<String> permNames = permissionMapper.selectByRoleId(role.getId())
                .stream()
                .map(p -> p.getName())
                .toList();
        vo.setPermissionNames(permNames);

        return vo;
    }
}
