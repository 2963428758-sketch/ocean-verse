package com.oceanverse.auth.service.impl;

import com.oceanverse.auth.service.CaptchaService;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.pojo.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final long CAPTCHA_TTL = 300L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisUtil redisUtil;

    @Override
    public CaptchaVO generate() {
        int a = RANDOM.nextInt(50) + 1;
        int b = RANDOM.nextInt(50) + 1;
        boolean plus = RANDOM.nextBoolean();
        int answer = plus ? a + b : a - b;
        String expression = a + (plus ? " + " : " - ") + b + " = ?";

        String captchaKey = UUID.randomUUID().toString();
        String redisKey = CommonConstants.REDIS_CAPTCHA + captchaKey;
        redisUtil.set(redisKey, String.valueOf(answer), CAPTCHA_TTL);

        log.debug("验证码生成: key={}, answer={}", captchaKey, answer);
        return new CaptchaVO(captchaKey, expression, CAPTCHA_TTL);
    }

    @Override
    public void verify(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaKey.isBlank()) {
            throw new BusinessException("验证码标识不能为空");
        }
        if (captchaCode == null || captchaCode.isBlank()) {
            throw new BusinessException("验证码不能为空");
        }

        String redisKey = CommonConstants.REDIS_CAPTCHA + captchaKey;
        String stored = redisUtil.get(redisKey);
        if (stored == null) {
            throw new BusinessException("验证码已过期，请刷新重试");
        }

        redisUtil.delete(redisKey);

        if (!stored.equals(captchaCode.trim())) {
            throw new BusinessException("验证码不正确");
        }
    }

    @Override
    public void clear(String captchaKey) {
        if (captchaKey != null && !captchaKey.isBlank()) {
            redisUtil.delete(CommonConstants.REDIS_CAPTCHA + captchaKey);
        }
    }
}
