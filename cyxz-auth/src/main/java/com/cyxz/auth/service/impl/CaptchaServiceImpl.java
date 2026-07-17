package com.cyxz.auth.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.cyxz.auth.service.CaptchaService;
import com.cyxz.common.constant.CacheKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现
 * <p>生成图形验证码并缓存至 Redis。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成图形验证码
     * <p>返回 Base64 图片及 UUID，正确码值存入 Redis，5 分钟有效。
     *
     * @return uuid + Base64 图片
     */
    @Override
    public Map<String, String> generateCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 44, 4, 45);

        String code = captcha.getCode();
        String imageBase64 = captcha.getImageBase64Data();
        String uuid = IdUtil.fastSimpleUUID();

        stringRedisTemplate.opsForValue().set(
                CacheKeyConstants.getCaptchaKey(uuid),
                code.toLowerCase(),
                CacheKeyConstants.CAPTCHA_EXPIRE_MINUTES,
                TimeUnit.MINUTES);

        log.info("生成验证码: uuid={}", uuid);

        Map<String, String> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("image", imageBase64);

        return result;
    }
}
