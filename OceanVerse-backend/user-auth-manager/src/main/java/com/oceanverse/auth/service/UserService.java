package com.oceanverse.auth.service;

import com.oceanverse.pojo.dto.LoginDTO;
import com.oceanverse.pojo.dto.RegisterDTO;

import java.util.Map;

/**
 * 用户服务接口 — 成员A
 */
public interface UserService {

    /**
     * 登录，返回 token 和用户信息
     */
    Map<String, Object> login(LoginDTO dto);

    /**
     * 注册新用户
     */
    void register(RegisterDTO dto);

    /**
     * 退出登录
     */
    void logout(String token);

    /**
     * 获取当前用户信息
     */
    Map<String, Object> getUserInfo(String token);
}
