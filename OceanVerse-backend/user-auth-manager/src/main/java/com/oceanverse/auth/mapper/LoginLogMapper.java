package com.oceanverse.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oceanverse.pojo.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper — 预留接口，供未来审计日志功能使用
 * TODO: 实现登录日志记录 Service 和 Controller
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<SysLoginLog> {
}
