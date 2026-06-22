package com.oceanverse.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.auth.mapper.LoginLogMapper;
import com.oceanverse.auth.mapper.PermissionMapper;
import com.oceanverse.auth.mapper.RoleMapper;
import com.oceanverse.auth.mapper.UserMapper;
import com.oceanverse.auth.mapper.UserRoleMapper;
import com.oceanverse.auth.service.CaptchaService;
import com.oceanverse.auth.service.UserService;
import com.oceanverse.auth.validator.PasswordStrengthValidator;
import com.oceanverse.auth.validator.ValidationResult;
import com.oceanverse.auth.event.AuthEventPublisher;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.common.result.PageResult;
import com.oceanverse.common.utils.JwtUtil;
import com.oceanverse.common.utils.OssUtil;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.pojo.dto.AssignRolesDTO;
import com.oceanverse.pojo.dto.LoginDTO;
import com.oceanverse.pojo.dto.RegisterDTO;
import com.oceanverse.pojo.dto.UpdatePasswordDTO;
import com.oceanverse.pojo.dto.UpdateProfileDTO;
import com.oceanverse.pojo.dto.UploadResult;
import com.oceanverse.pojo.entity.SysLoginLog;
import com.oceanverse.pojo.entity.SysRole;
import com.oceanverse.pojo.entity.SysUserRole;
import com.oceanverse.pojo.entity.User;
import com.oceanverse.pojo.vo.LoginVO;
import com.oceanverse.pojo.vo.UserInfoVO;
import com.oceanverse.pojo.vo.UserListVO;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

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
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordStrengthValidator passwordValidator;
    private final RedisUtil redisUtil;
    private final AuthEventPublisher authEventPublisher;
    private final CaptchaService captchaService;
    private final OssUtil ossUtil;
    private final TransactionTemplate transactionTemplate;

    /**
     * 手动创建 REQUIRES_NEW 事务模板，确保锁定/日志写入不被外层回滚
     */
    private TransactionTemplate newTransaction() {
        TransactionTemplate tt = new TransactionTemplate(transactionTemplate.getTransactionManager());
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto, String clientIp, String userAgent) {
        String failKey = CommonConstants.REDIS_LOGIN_FAIL + dto.getUsername();

        // 1. 检查是否已被爆破锁定（登录前先查 IP 无关的全局锁）
        checkLockStatus(dto.getUsername(), failKey);

        // 2. 验证码校验
        captchaService.verify(dto.getCaptchaKey(), dto.getCaptchaCode());

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
                        .eq(User::getDeleted, CommonConstants.NOT_DELETED)
        );

        // 用户不存在 → 二次检查是否已注销
        if (user == null) {
            long deletedCount = userMapper.countByUsernameIgnoreDeleted(dto.getUsername());
            if (deletedCount > 0) {
                throw new BusinessException("该账号已注销，30天后可重新注册");
            }
            throw new BusinessException("该用户不存在，请先注册");
        }

        // 密码错误
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            recordFailedAttempt(dto.getUsername(), failKey, user);
            recordLoginLog(user, dto.getUsername(), clientIp, userAgent, 0, "密码错误");
            throw new BusinessException("密码错误");
        }

        // 账号已禁用
        if (user.getStatus() == CommonConstants.USER_STATUS_DISABLED) {
            recordLoginLog(user, dto.getUsername(), clientIp, userAgent, 0, "账户已被禁用");
            throw new BusinessException("账户已被禁用，请联系管理员");
        }

        // 账号被锁定（检查 Redis TTL，自动解封）
        if (user.getStatus() == CommonConstants.USER_STATUS_LOCKED) {
            Long lockTtl = redisUtil.getExpire(failKey);
            if (lockTtl != null && lockTtl > 0) {
                recordLoginLog(user, dto.getUsername(), clientIp, userAgent, 0, "账户已锁定");
                throw new BusinessException("账户已被锁定，请 " + lockTtl + " 秒后重试");
            }
            // TTL 过期 → 自动解锁
            user.setStatus(CommonConstants.USER_STATUS_NORMAL);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("账户自动解锁: {}", dto.getUsername());
        }

        // 登录成功 → 清除失败计数
        redisUtil.delete(failKey);

        String roleCode = userMapper.selectRoleCodeByUserId(user.getId());
        if (roleCode == null) {
            roleCode = "VIEWER";
        }

        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleCode, user.getDataScope());
        String refreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getUsername(), roleCode, user.getDataScope());

        redisUtil.set(CommonConstants.REDIS_USER_TOKEN + user.getId(), accessToken, CommonConstants.ACCESS_TOKEN_TTL);
        redisUtil.set(CommonConstants.REDIS_USER_REFRESH_TOKEN + user.getId(), refreshToken, CommonConstants.REFRESH_TOKEN_TTL);

        cacheUserPermissions(user.getId());

        // 异地登录检测
        String previousIp = user.getLastLoginIp();
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userMapper.updateById(user);

        if (previousIp != null && !previousIp.equals(clientIp) && !isLocalIp(clientIp)) {
            authEventPublisher.publishLoginAlert(user.getId(), user.getUsername(), clientIp, previousIp);
            log.warn("异地登录告警: username={}, previousIp={}, currentIp={}", user.getUsername(), previousIp, clientIp);
        }

        recordLoginLog(user, dto.getUsername(), clientIp, userAgent, 1, "登录成功");

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
        // 验证码校验
        captchaService.verify(dto.getCaptchaKey(), dto.getCaptchaCode());

        // 密码强度校验
        ValidationResult vr = passwordValidator.validate(dto.getPassword(), dto.getUsername());
        if (!vr.isValid()) {
            throw new BusinessException(vr.getMessage());
        }

        long count = userMapper.countByUsernameIgnoreDeleted(dto.getUsername());
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName("");
        user.setStatus(CommonConstants.USER_STATUS_NORMAL);
        user.setDataScope(CommonConstants.DATA_SCOPE_SELF);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 分配默认角色 VIEWER
        int inserted = userMapper.insertUserRole(user.getId(), "VIEWER");
        if (inserted == 0) {
            log.error("新用户注册分配默认角色失败: userId={}, roleCode=VIEWER（角色可能不存在）", user.getId());
            throw new BusinessException("系统初始化异常，请联系管理员");
        }

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
            log.warn("退出登录处理异常: {}", e.getMessage(), e);
        }
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        String roleCode = userMapper.selectRoleCodeByUserId(userId);

        // 从 Redis 缓存加载权限列表
        @SuppressWarnings("unchecked")
        List<String> permissions = redisUtil.getObject(
                CommonConstants.REDIS_USER_PERMS + userId, List.class);
        if (permissions == null) {
            // 缓存未命中，从 DB 加载并缓存
            permissions = permissionMapper.selectPermCodesByUserId(userId);
            if (permissions == null) {
                permissions = Collections.emptyList();
            }
            redisUtil.setObject(CommonConstants.REDIS_USER_PERMS + userId, permissions, CommonConstants.PERMS_CACHE_TTL);
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRealName(user.getRealName());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(roleCode);
        vo.setPermissions(permissions);
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

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
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
        // 密码强度校验
        ValidationResult vr = passwordValidator.validate(dto.getNewPassword(), null);
        if (!vr.isValid()) {
            throw new BusinessException(vr.getMessage());
        }

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

        // 推送密码修改安全提醒
        authEventPublisher.publishPasswordChange(userId, user.getUsername());

        log.info("用户修改密码: userId={}, 已清除所有Token", userId);
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!JwtUtil.validate(refreshToken)) {
            throw new BusinessException(401, "RefreshToken 已过期或无效");
        }
        if (!JwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(401, "token 类型错误，需要 RefreshToken");
        }

        Long userId = JwtUtil.getUserId(refreshToken);
        String username = JwtUtil.getUsername(refreshToken);
        String role = JwtUtil.getRole(refreshToken);
        Integer dataScope = JwtUtil.getDataScope(refreshToken);

        String cachedRefresh = redisUtil.get(CommonConstants.REDIS_USER_REFRESH_TOKEN + userId);
        if (cachedRefresh == null || !cachedRefresh.equals(refreshToken)) {
            throw new BusinessException(401, "RefreshToken 已失效，请重新登录");
        }

        // 将旧 AccessToken 加入黑名单，防止被截获后继续使用
        String oldAccessToken = redisUtil.get(CommonConstants.REDIS_USER_TOKEN + userId);
        if (oldAccessToken != null) {
            String oldTokenHash = DigestUtils.md5DigestAsHex(oldAccessToken.getBytes());
            long remainingSeconds = JwtUtil.getRemainingSeconds(oldAccessToken);
            if (remainingSeconds > 0) {
                redisUtil.set(CommonConstants.REDIS_TOKEN_BLACKLIST + oldTokenHash, "1", remainingSeconds);
            }
        }

        String newAccessToken = JwtUtil.generateAccessToken(userId, username, role, dataScope);
        String newRefreshToken = JwtUtil.generateRefreshToken(userId, username, role, dataScope);

        redisUtil.set(CommonConstants.REDIS_USER_TOKEN + userId, newAccessToken, CommonConstants.ACCESS_TOKEN_TTL);
        redisUtil.set(CommonConstants.REDIS_USER_REFRESH_TOKEN + userId, newRefreshToken, CommonConstants.REFRESH_TOKEN_TTL);

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
                    .or().like(User::getRealName, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        wrapper.eq(User::getDeleted, CommonConstants.NOT_DELETED);
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
            vo.setRealName(user.getRealName());
            vo.setAvatarUrl(user.getAvatarUrl());
            vo.setStatus(user.getStatus());
            vo.setDataScope(user.getDataScope());
            vo.setLastLoginTime(user.getLastLoginTime());
            vo.setCreateTime(user.getCreateTime());
            vo.setRole(roleMap.getOrDefault(user.getId(), "VIEWER"));

            // 被锁定用户：查询 Redis 剩余锁定时间
            if (user.getStatus() == CommonConstants.USER_STATUS_LOCKED) {
                Long ttl = redisUtil.getExpire(CommonConstants.REDIS_LOGIN_FAIL + user.getUsername());
                vo.setLockRemainingSeconds(ttl != null && ttl > 0 ? ttl : 0L);
            }
            return vo;
        }).toList();

        return PageResult.of(voList, result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, int status) {
        // 防止管理员禁用/锁定自己
        Long currentUserId = com.oceanverse.auth.context.UserContext.getUserId();
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new BusinessException("不能修改自己的账户状态");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }
        Integer oldStatus = user.getStatus();
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

        // 解冻：清除 Redis 爆破锁定计数，防止立即被再次拦截
        if (status == CommonConstants.USER_STATUS_NORMAL && oldStatus == CommonConstants.USER_STATUS_LOCKED) {
            redisUtil.delete(CommonConstants.REDIS_LOGIN_FAIL + user.getUsername());
            log.info("管理员解冻用户，已清除 Redis 锁定计数: username={}", user.getUsername());
        }

        log.info("管理员修改用户状态: userId={}, status={}", userId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(AssignRolesDTO dto) {
        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        // 校验所有角色ID存在性
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(dto.getRoleIds());
            if (roles.size() != dto.getRoleIds().size()) {
                throw new BusinessException("存在无效的角色ID");
            }
        }

        // 先删后插（全量替换用户角色）
        userRoleMapper.deleteByUserId(dto.getUserId());

        for (Long roleId : dto.getRoleIds()) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(dto.getUserId());
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }

        // 清除该用户的权限和角色缓存，下次请求时重新加载
        redisUtil.delete(CommonConstants.REDIS_USER_PERMS + dto.getUserId());
        redisUtil.delete(CommonConstants.REDIS_USER_ROLES + dto.getUserId());

        log.info("用户角色分配: userId={}, roleIds={}", dto.getUserId(), dto.getRoleIds());
    }

    @Override
    public void forceLogout(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        // 1. 清除 Redis 权限缓存
        redisUtil.delete(CommonConstants.REDIS_USER_PERMS + userId);
        redisUtil.delete(CommonConstants.REDIS_USER_ROLES + userId);

        // 2. 将当前 AccessToken 加入黑名单
        String accessToken = redisUtil.get(CommonConstants.REDIS_USER_TOKEN + userId);
        if (accessToken != null) {
            String tokenHash = DigestUtils.md5DigestAsHex(accessToken.getBytes());
            long remainingSeconds = JwtUtil.getRemainingSeconds(accessToken);
            if (remainingSeconds > 0) {
                redisUtil.set(CommonConstants.REDIS_TOKEN_BLACKLIST + tokenHash, "1", remainingSeconds);
            }
        }

        // 3. 清除 Token 缓存
        redisUtil.delete(CommonConstants.REDIS_USER_TOKEN + userId);
        redisUtil.delete(CommonConstants.REDIS_USER_REFRESH_TOKEN + userId);

        // 4. 推送强制下线通知
        authEventPublisher.publishForceLogout(userId, user.getUsername());

        log.info("强制下线: userId={}, username={}", userId, user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResult uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持图片格式");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("头像大小不能超过2MB");
        }

        String url = ossUtil.upload(file);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }
        user.setAvatarUrl(url);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户上传头像: userId={}, url={}", userId, url);
        return UploadResult.of(url, file.getOriginalFilename(), file.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        // 软删除（绕过 @TableLogic 对 updateById 的拦截，显式写入 deleted）
        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getDeleted, CommonConstants.DELETED)
                        .set(User::getUpdateTime, LocalDateTime.now()));

        // 当前 Token 加入黑名单（必须在 delete 之前）
        String accessToken = redisUtil.get(CommonConstants.REDIS_USER_TOKEN + userId);
        if (accessToken != null) {
            String tokenHash = DigestUtils.md5DigestAsHex(accessToken.getBytes());
            long remaining = JwtUtil.getRemainingSeconds(accessToken);
            if (remaining > 0) {
                redisUtil.set(CommonConstants.REDIS_TOKEN_BLACKLIST + tokenHash, "1", remaining);
            }
        }

        // 清除所有 Redis 缓存
        redisUtil.delete(CommonConstants.REDIS_USER_TOKEN + userId);
        redisUtil.delete(CommonConstants.REDIS_USER_REFRESH_TOKEN + userId);
        redisUtil.delete(CommonConstants.REDIS_USER_PERMS + userId);
        redisUtil.delete(CommonConstants.REDIS_USER_ROLES + userId);

        log.info("用户注销账号: userId={}, username={}", userId, user.getUsername());
    }

    @Override
    public PageResult<SysLoginLog> getLoginHistory(Long userId, int page, int size) {
        Page<SysLoginLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysLoginLog::getUserId, userId)
                .orderByDesc(SysLoginLog::getLoginTime);
        Page<SysLoginLog> result = loginLogMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    /**
     * 缓存用户权限和角色到 Redis（TTL 与 AccessToken 一致 = 2h）
     */
    private void cacheUserPermissions(Long userId) {
        try {
            List<String> permCodes = permissionMapper.selectPermCodesByUserId(userId);
            if (permCodes == null) {
                permCodes = Collections.emptyList();
            }
            redisUtil.setObject(CommonConstants.REDIS_USER_PERMS + userId, permCodes, CommonConstants.PERMS_CACHE_TTL);

            List<String> roles = userMapper.selectRolesByUserId(userId);
            if (roles == null || roles.isEmpty()) {
                roles = List.of("VIEWER");
            }
            redisUtil.setObject(CommonConstants.REDIS_USER_ROLES + userId, roles, CommonConstants.PERMS_CACHE_TTL);
        } catch (Exception e) {
            log.warn("缓存用户权限失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 检查爆破锁定状态，若 Redis TTL 已过期则自动解锁
     */
    private void checkLockStatus(String username, String failKey) {
        Long failCount = redisUtil.getLong(failKey);
        if (failCount != null && failCount >= CommonConstants.LOGIN_FAIL_MAX_ATTEMPTS) {
            Long lockTtl = redisUtil.getExpire(failKey);
            if (lockTtl != null && lockTtl > 0) {
                throw new BusinessException("账户已被锁定，请 " + lockTtl + " 秒后重试");
            }
            // TTL 已过期，自动清除限制
            redisUtil.delete(failKey);
        }
    }

    /**
     * 记录登录失败，达上限后锁定账号（独立事务，避免被外层回滚）
     */
    private void recordFailedAttempt(String username, String failKey, User user) {
        try {
            long attempts = redisUtil.incr(failKey, CommonConstants.LOGIN_LOCK_SECONDS);
            if (attempts >= CommonConstants.LOGIN_FAIL_MAX_ATTEMPTS && user != null) {
                newTransaction().execute(status -> {
                    user.setStatus(CommonConstants.USER_STATUS_LOCKED);
                    user.setUpdateTime(LocalDateTime.now());
                    userMapper.updateById(user);
                    return null;
                });
                log.warn("账户爆破锁定: username={}, attempts={}", username, attempts);
            }
        } catch (Exception e) {
            log.warn("登录失败计数异常: {}", e.getMessage());
        }
    }

    /**
     * 写入登录日志（独立事务，避免被外层回滚）
     */
    private void recordLoginLog(User user, String username, String clientIp, String userAgent, int status, String message) {
        try {
            newTransaction().execute(txStatus -> {
                SysLoginLog logEntry = new SysLoginLog();
                logEntry.setUserId(user != null ? user.getId() : null);
                logEntry.setUsername(username);
                logEntry.setIpAddress(clientIp);
                logEntry.setUserAgent(userAgent);
                logEntry.setStatus(status);
                logEntry.setMessage(message);
                logEntry.setLoginTime(LocalDateTime.now());
                loginLogMapper.insert(logEntry);
                return null;
            });
        } catch (Exception e) {
            log.warn("登录日志写入失败: {}", e.getMessage());
        }
    }

    /**
     * 判断是否为本地/内网 IP（用于跳过异地登录告警）
     */
    private boolean isLocalIp(String ip) {
        if (ip == null) return true;
        return ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")
                || ip.startsWith("192.168.") || ip.startsWith("10.")
                || ip.startsWith("172.16.") || ip.startsWith("172.17.")
                || ip.startsWith("172.18.") || ip.startsWith("172.19.")
                || ip.startsWith("172.20.") || ip.startsWith("172.21.")
                || ip.startsWith("172.22.") || ip.startsWith("172.23.")
                || ip.startsWith("172.24.") || ip.startsWith("172.25.")
                || ip.startsWith("172.26.") || ip.startsWith("172.27.")
                || ip.startsWith("172.28.") || ip.startsWith("172.29.")
                || ip.startsWith("172.30.") || ip.startsWith("172.31.");
    }
}
