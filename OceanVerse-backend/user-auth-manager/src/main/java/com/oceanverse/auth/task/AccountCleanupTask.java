package com.oceanverse.auth.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oceanverse.auth.mapper.UserMapper;
import com.oceanverse.auth.mapper.UserRoleMapper;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.pojo.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账号清理定时任务 — 每天凌晨3点物理删除过期注销账号（注销超过30天）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCleanupTask {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredAccounts() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<User> expiredUsers = userMapper.selectExpiredDeletedUsers(thirtyDaysAgo);

        if (expiredUsers.isEmpty()) {
            return;
        }

        for (User user : expiredUsers) {
            userRoleMapper.deleteByUserId(user.getId());
            userMapper.physicalDeleteById(user.getId());
            log.info("物理删除过期注销账号: userId={}, username={}", user.getId(), user.getUsername());
        }

        log.info("本次清理 {} 个过期注销账号", expiredUsers.size());
    }
}
