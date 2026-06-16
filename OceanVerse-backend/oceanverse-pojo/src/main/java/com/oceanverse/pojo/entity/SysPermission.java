package com.oceanverse.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统权限实体
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父权限ID，0 表示顶级 */
    private Long parentId;

    /** 权限名称 */
    private String name;

    /** 权限标识，如 user:list */
    private String permCode;

    /** 类型: MENU-菜单, BUTTON-按钮, API-接口 */
    private String type;

    /** 排序号 */
    private Integer sort;

    /** 描述 */
    private String description;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
