package com.oceanverse.auth.controller;

import com.oceanverse.auth.service.CaptchaService;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.vo.CaptchaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Captcha", description = "验证码接口")
@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @Operation(summary = "获取验证码", description = "生成图文算式验证码，返回 captchaKey + 表达式，有效期5分钟")
    @GetMapping
    public Result<CaptchaVO> getCaptcha() {
        return Result.success(captchaService.generate());
    }
}
