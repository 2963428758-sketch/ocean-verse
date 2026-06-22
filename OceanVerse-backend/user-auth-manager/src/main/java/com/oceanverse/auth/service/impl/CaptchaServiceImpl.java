package com.oceanverse.auth.service.impl;

import com.oceanverse.auth.service.CaptchaService;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.pojo.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final long CAPTCHA_TTL = 300L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHAR_POOL = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int WIDTH = 130;
    private static final int HEIGHT = 50;

    private final RedisUtil redisUtil;

    @Override
    public CaptchaVO generate() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        String answer = code.toString();

        String imageBase64 = generateImage(answer);

        String captchaKey = UUID.randomUUID().toString();
        String redisKey = CommonConstants.REDIS_CAPTCHA + captchaKey;
        redisUtil.set(redisKey, answer, CAPTCHA_TTL);

        log.debug("图形验证码生成: key={}", captchaKey);
        return new CaptchaVO(captchaKey, imageBase64, CAPTCHA_TTL);
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

        if (!stored.equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException("验证码不正确");
        }
    }

    @Override
    public void clear(String captchaKey) {
        if (captchaKey != null && !captchaKey.isBlank()) {
            redisUtil.delete(CommonConstants.REDIS_CAPTCHA + captchaKey);
        }
    }

    private String generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 随机浅色背景
        g2d.setColor(new Color(240 + RANDOM.nextInt(15), 240 + RANDOM.nextInt(15), 240 + RANDOM.nextInt(15)));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        Font[] fonts = {
                new Font("Arial", Font.BOLD, 30),
                new Font("Arial", Font.ITALIC, 28),
                new Font("Times New Roman", Font.BOLD, 32),
        };

        // 绘制字符（带随机旋转和偏移）
        for (int i = 0; i < code.length(); i++) {
            Font font = fonts[RANDOM.nextInt(fonts.length)];
            g2d.setFont(font);
            g2d.setColor(new Color(20 + RANDOM.nextInt(80), 20 + RANDOM.nextInt(100), 20 + RANDOM.nextInt(120)));

            AffineTransform orig = g2d.getTransform();
            double angle = Math.toRadians(-18 + RANDOM.nextInt(36));
            int cx = 20 + i * 28;
            int cy = 28 + RANDOM.nextInt(12);
            g2d.rotate(angle, cx, cy);
            g2d.drawString(String.valueOf(code.charAt(i)), cx - 8, cy + 8);
            g2d.setTransform(orig);
        }

        // 干扰线
        for (int i = 0; i < 5; i++) {
            g2d.setColor(new Color(100 + RANDOM.nextInt(155), 100 + RANDOM.nextInt(155), 100 + RANDOM.nextInt(155)));
            g2d.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                    RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }

        // 噪点
        for (int i = 0; i < 150; i++) {
            g2d.setColor(new Color(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255)));
            g2d.fillOval(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
        }

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
