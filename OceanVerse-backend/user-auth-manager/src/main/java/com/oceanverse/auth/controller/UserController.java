package com.oceanverse.auth.controller;

import com.oceanverse.auth.context.UserContext;
import com.oceanverse.auth.service.UserService;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.UpdatePasswordDTO;
import com.oceanverse.pojo.dto.UpdateProfileDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        Long userId = UserContext.getUserId();
        userService.updateProfile(userId, dto);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        Long userId = UserContext.getUserId();
        userService.updatePassword(userId, dto);
        return Result.success();
    }
}
