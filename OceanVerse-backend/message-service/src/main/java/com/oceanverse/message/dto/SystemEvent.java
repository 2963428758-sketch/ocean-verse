package com.oceanverse.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统事件 DTO — 用于 stream:system
 * <p>
 * 任意模块可发布系统级事件，如缓存刷新、统计更新、用户注册等，
 * 由 SystemStreamConsumer 消费并执行对应操作。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemEvent {

    /** 事件动作: CACHE_REFRESH / STATS_UPDATE / LOG_ARCHIVE / USER_REGISTER */
    private String action;

    /** 缓存 key 或目标标识 */
    private String targetKey;

    /** 附加数据（可选） */
    private String data;
}
