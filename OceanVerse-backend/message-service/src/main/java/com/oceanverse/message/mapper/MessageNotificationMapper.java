package com.oceanverse.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oceanverse.pojo.entity.SysNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统通知 Mapper（message-service 模块专用）
 */
@Mapper
public interface MessageNotificationMapper extends BaseMapper<SysNotification> {
}
