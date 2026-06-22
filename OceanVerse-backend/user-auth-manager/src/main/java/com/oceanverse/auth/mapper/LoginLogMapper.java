package com.oceanverse.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oceanverse.pojo.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<SysLoginLog> {
}
