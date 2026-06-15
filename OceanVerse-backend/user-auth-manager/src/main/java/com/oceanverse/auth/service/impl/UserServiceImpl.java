package com.oceanverse.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oceanverse.auth.mapper.UserMapper;
import com.oceanverse.auth.service.UserService;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.common.utils.JwtUtil;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.pojo.dto.LoginDTO;
import com.oceanverse.pojo.dto.RegisterDTO;
import com.oceanverse.pojo.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        // 1. 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
                        .eq(User::getDeleted, CommonConstants.NOT_DELETED)
        );
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 检查状态
        if (user.getStatus() != CommonConstants.USER_STATUS_NORMAL) {
            throw new BusinessException("账户已被禁用或锁定");
        }

        // 4. 生成 token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), "USER");

        // 5. 缓存用户信息到 Redis
        redisUtil.set(CommonConstants.REDIS_USER_TOKEN + user.getId(), token, 86400);

        // 6. 更新登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 7. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("avatarUrl", user.getAvatarUrl());

        log.info("用户登录成功: {}", user.getUsername());
        return result;
    }

    @Override
    public void register(RegisterDTO dto) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已存在
        count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
        );
        if (count > 0) {
            throw new BusinessException("邮箱已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(CommonConstants.USER_STATUS_NORMAL);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        log.info("新用户注册: {}", dto.getUsername());
    }

    @Override
    public void logout(String token) {
        try {
            Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
            redisUtil.delete(CommonConstants.REDIS_USER_TOKEN + userId);
            log.info("用户退出登录: userId={}", userId);
        } catch (Exception e) {
            log.warn("退出登录处理异常: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("email", user.getEmail());
        info.put("phone", user.getPhone());
        info.put("realName", user.getRealName());
        info.put("avatarUrl", user.getAvatarUrl());
        info.put("status", user.getStatus());
        info.put("createTime", user.getCreateTime());
        return info;
    }
}
