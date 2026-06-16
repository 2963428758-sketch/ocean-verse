package com.oceanverse.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.auth.mapper.UserMapper;
import com.oceanverse.auth.service.UserService;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.common.result.PageResult;
import com.oceanverse.common.utils.JwtUtil;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.pojo.dto.LoginDTO;
import com.oceanverse.pojo.dto.RegisterDTO;
import com.oceanverse.pojo.dto.UpdatePasswordDTO;
import com.oceanverse.pojo.dto.UpdateProfileDTO;
import com.oceanverse.pojo.entity.User;
import com.oceanverse.pojo.vo.LoginVO;
import com.oceanverse.pojo.vo.UserInfoVO;
import com.oceanverse.pojo.vo.UserListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto, String clientIp) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
                        .eq(User::getDeleted, CommonConstants.NOT_DELETED)
        );
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == CommonConstants.USER_STATUS_DISABLED) {
            throw new BusinessException("账户已被禁用，请联系管理员");
        }
        if (user.getStatus() == CommonConstants.USER_STATUS_LOCKED) {
            throw new BusinessException("账户已被锁定，请稍后再试或联系管理员");
        }

        String roleCode = userMapper.selectRoleCodeByUserId(user.getId());
        if (roleCode == null) {
            roleCode = "VIEWER";
        }

        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleCode, user.getDataScope());
        String refreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getUsername(), roleCode);

        redisUtil.set(CommonConstants.REDIS_USER_TOKEN + user.getId(), accessToken, 7200);
        redisUtil.set(CommonConstants.REDIS_USER_REFRESH_TOKEN + user.getId(), refreshToken, 604800);

        // 更新最后登录时间和IP
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userMapper.updateById(user);

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(roleCode);
        vo.setAvatarUrl(user.getAvatarUrl());

        log.info("用户登录成功: {}, role={}, ip={}", user.getUsername(), roleCode, clientIp);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
        );
        if (count > 0) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(CommonConstants.USER_STATUS_NORMAL);
        user.setDataScope(CommonConstants.DATA_SCOPE_SELF);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 分配默认角色 VIEWER
        userMapper.insertUserRole(user.getId(), "VIEWER");

        log.info("新用户注册: {}, 已分配默认角色 VIEWER", dto.getUsername());
    }

    @Override
    public void logout(String token) {
        try {
            String rawToken = token.replace("Bearer ", "");
            Long userId = JwtUtil.getUserId(rawToken);

            // Token 加入黑名单，TTL 为 Token 剩余有效时间
            long remainingSeconds = JwtUtil.getRemainingSeconds(rawToken);
            if (remainingSeconds > 0) {
                String tokenHash = org.springframework.util.DigestUtils.md5DigestAsHex(rawToken.getBytes());
                redisUtil.set(CommonConstants.REDIS_TOKEN_BLACKLIST + tokenHash, "1", remainingSeconds);
            }

            redisUtil.delete(CommonConstants.REDIS_USER_TOKEN + userId);
            redisUtil.delete(CommonConstants.REDIS_USER_REFRESH_TOKEN + userId);
            log.info("用户退出登录: userId={}, token已加入黑名单", userId);
        } catch (Exception e) {
            log.warn("退出登录处理异常: {}", e.getMessage());
        }
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        String roleCode = userMapper.selectRoleCodeByUserId(userId);

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setRealName(user.getRealName());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(roleCode);
        vo.setPermissions(Collections.emptyList());
        vo.setDataScope(user.getDataScope());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        if (dto.getEmail() != null) {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getEmail, dto.getEmail())
                            .ne(User::getId, userId)
            );
            if (count > 0) {
                throw new BusinessException("邮箱已被其他用户使用");
            }
            user.setEmail(dto.getEmail());
        }
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户更新个人信息: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 修改密码后清除所有 Token，强制重新登录
        redisUtil.delete(CommonConstants.REDIS_USER_TOKEN + userId);
        redisUtil.delete(CommonConstants.REDIS_USER_REFRESH_TOKEN + userId);
        redisUtil.delete(CommonConstants.REDIS_USER_PERMS + userId);
        redisUtil.delete(CommonConstants.REDIS_USER_ROLES + userId);

        log.info("用户修改密码: userId={}, 已清除所有Token", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO refreshToken(String refreshToken) {
        if (!JwtUtil.validate(refreshToken)) {
            throw new BusinessException(401, "RefreshToken 已过期或无效");
        }
        if (!JwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(401, "token 类型错误，需要 RefreshToken");
        }

        Long userId = JwtUtil.getUserId(refreshToken);
        String cachedRefresh = redisUtil.get(CommonConstants.REDIS_USER_REFRESH_TOKEN + userId);
        if (cachedRefresh == null || !cachedRefresh.equals(refreshToken)) {
            throw new BusinessException(401, "RefreshToken 已失效，请重新登录");
        }

        String username = JwtUtil.getUsername(refreshToken);
        String role = JwtUtil.getRole(refreshToken);

        String newAccessToken = JwtUtil.generateAccessToken(userId, username, role);
        String newRefreshToken = JwtUtil.generateRefreshToken(userId, username, role);

        redisUtil.set(CommonConstants.REDIS_USER_TOKEN + userId, newAccessToken, 7200);
        redisUtil.set(CommonConstants.REDIS_USER_REFRESH_TOKEN + userId, newRefreshToken, 604800);

        LoginVO vo = new LoginVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setRole(role);

        log.info("Token 刷新成功: userId={}", userId);
        return vo;
    }

    @Override
    public PageResult<UserListVO> listUsers(int page, int size, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getEmail, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(pageParam, wrapper);

        List<User> records = result.getRecords();

        // 批量查询所有用户的角色（解决 N+1 问题）
        List<Long> userIds = records.stream().map(User::getId).toList();
        Map<Long, String> roleMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<Map<String, Object>> roleRows = userMapper.selectRoleMapByUserIds(userIds);
            for (Map<String, Object> row : roleRows) {
                Long uid = ((Number) row.get("user_id")).longValue();
                if (!roleMap.containsKey(uid)) {
                    roleMap.put(uid, (String) row.get("role_code"));
                }
            }
        }

        List<UserListVO> voList = records.stream().map(user -> {
            UserListVO vo = new UserListVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setRealName(user.getRealName());
            vo.setAvatarUrl(user.getAvatarUrl());
            vo.setStatus(user.getStatus());
            vo.setDataScope(user.getDataScope());
            vo.setLastLoginTime(user.getLastLoginTime());
            vo.setCreateTime(user.getCreateTime());
            vo.setRole(roleMap.getOrDefault(user.getId(), "VIEWER"));
            return vo;
        }).toList();

        return PageResult.of(voList, result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, int status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 如果禁用/锁定用户，清除其权限缓存和 Token
        if (status != CommonConstants.USER_STATUS_NORMAL) {
            redisUtil.delete(CommonConstants.REDIS_USER_TOKEN + userId);
            redisUtil.delete(CommonConstants.REDIS_USER_REFRESH_TOKEN + userId);
            redisUtil.delete(CommonConstants.REDIS_USER_PERMS + userId);
            redisUtil.delete(CommonConstants.REDIS_USER_ROLES + userId);
        }

        log.info("管理员修改用户状态: userId={}, status={}", userId, status);
    }
}
