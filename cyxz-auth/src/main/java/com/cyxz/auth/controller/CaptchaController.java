package com.cyxz.auth.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.cyxz.common.base.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class CaptchaController {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;

    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 44, 4, 45);

        String code = captcha.getCode();
        String imageBase64 = captcha.getImageBase64Data();
        String uuid = IdUtil.fastSimpleUUID();

        stringRedisTemplate.opsForValue().set(CAPTCHA_PREFIX + uuid, code.toLowerCase(), CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("生成验证码: uuid={}", uuid);

        Map<String, String> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("image", imageBase64);

        return Result.success(result);
    }
}
