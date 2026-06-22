package com.oceanverse.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oceanverse.pojo.entity.SysOperateLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface OperateLogMapper extends BaseMapper<SysOperateLog> {
}
