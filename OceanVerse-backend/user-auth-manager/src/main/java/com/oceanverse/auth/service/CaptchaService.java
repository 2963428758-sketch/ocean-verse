package com.oceanverse.auth.service;

import com.oceanverse.pojo.vo.CaptchaVO;

public interface CaptchaService {

    CaptchaVO generate();

    void verify(String captchaKey, String captchaCode);

    void clear(String captchaKey);
}
