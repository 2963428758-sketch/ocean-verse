package com.oceanverse.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileDTO {
    @Size(max = 50, message = "真实姓名长度不超过50")
    private String realName;
    @Email(message = "邮箱格式不正确")
    private String email;
    private String phone;
    private String avatarUrl;
}
