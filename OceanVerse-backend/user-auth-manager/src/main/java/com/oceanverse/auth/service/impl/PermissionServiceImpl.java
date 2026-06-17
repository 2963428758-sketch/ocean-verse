package com.oceanverse.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oceanverse.auth.mapper.PermissionMapper;
import com.oceanverse.auth.service.PermissionService;
import com.oceanverse.pojo.entity.SysPermission;
import com.oceanverse.pojo.vo.PermissionTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionTreeVO> getPermissionTree() {
        // 查询所有权限，按 sort 排序
        List<SysPermission> allPerms = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSort)
        );

        // 转换为 VO
        List<PermissionTreeVO> voList = allPerms.stream()
                .map(this::toTreeVO)
                .toList();

        // 构建 ID → VO 映射
        Map<Long, PermissionTreeVO> voMap = new LinkedHashMap<>();
        for (PermissionTreeVO vo : voList) {
            voMap.put(vo.getId(), vo);
        }

        // 组装树形结构
        List<PermissionTreeVO> roots = new ArrayList<>();
        for (PermissionTreeVO vo : voList) {
            if (vo.getParentId() == null || vo.getParentId() == 0L) {
                roots.add(vo);
            } else {
                PermissionTreeVO parent = voMap.get(vo.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                } else {
                    // 父级不存在则作为根节点
                    roots.add(vo);
                }
            }
        }

        return roots;
    }

    @Override
    public List<String> getUserPermCodes(Long userId) {
        return permissionMapper.selectPermCodesByUserId(userId);
    }

    @Override
    public List<Long> getRolePermIds(Long roleId) {
        return permissionMapper.selectPermIdsByRoleId(roleId);
    }

    private PermissionTreeVO toTreeVO(SysPermission perm) {
        PermissionTreeVO vo = new PermissionTreeVO();
        vo.setId(perm.getId());
        vo.setParentId(perm.getParentId());
        vo.setName(perm.getName());
        vo.setPermCode(perm.getPermCode());
        vo.setType(perm.getType());
        vo.setSort(perm.getSort());
        vo.setDescription(perm.getDescription());
        return vo;
    }
}
