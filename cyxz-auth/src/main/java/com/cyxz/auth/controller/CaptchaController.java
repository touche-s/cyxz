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

/**
 * 验证码控制器
 * <p>生成图形验证码，Base64 返回前端，正确码值缓存至 Redis。
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class CaptchaController {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;

    /**
     * 获取图形验证码
     * <p>使用 Hutool 生成 4 位线条验证码，返回 Base64 图片及 UUID，
     * 正确码值通过 Redis 缓存 5 分钟，供登录/注册时校验。
     *
     * @return uuid + Base64 图片
     */
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
