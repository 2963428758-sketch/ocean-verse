package com.oceanverse.auth.service;

import com.oceanverse.common.result.PageResult;
import com.oceanverse.pojo.dto.LoginDTO;
import com.oceanverse.pojo.dto.RegisterDTO;
import com.oceanverse.pojo.dto.UpdatePasswordDTO;
import com.oceanverse.pojo.dto.UpdateProfileDTO;
import com.oceanverse.pojo.vo.LoginVO;
import com.oceanverse.pojo.vo.UserInfoVO;
import com.oceanverse.pojo.vo.UserListVO;

public interface UserService {

    LoginVO login(LoginDTO dto, String clientIp);

    void register(RegisterDTO dto);

    void logout(String token);

    UserInfoVO getUserInfo(Long userId);

    void updateProfile(Long userId, UpdateProfileDTO dto);

    void updatePassword(Long userId, UpdatePasswordDTO dto);

    LoginVO refreshToken(String refreshToken);

    PageResult<UserListVO> listUsers(int page, int size, String keyword);

    void updateUserStatus(Long userId, int status);
}
