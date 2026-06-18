package com.oceanverse.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oceanverse.pojo.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserPointsMapper extends BaseMapper<UserPoints> {

    /**
     * 查询积分排行榜 — 按用户聚合积分总和，关联用户表获取昵称和头像
     */
    @Select("SELECT up.user_id AS userId, SUM(up.points) AS totalPoints, " +
            "u.username AS username, u.avatar_url AS avatarUrl " +
            "FROM user_points up " +
            "LEFT JOIN sys_user u ON up.user_id = u.id " +
            "GROUP BY up.user_id, u.username, u.avatar_url " +
            "ORDER BY totalPoints DESC " +
            "LIMIT #{topN}")
    List<Map<String, Object>> selectLeaderboard(@Param("topN") int topN);
}
