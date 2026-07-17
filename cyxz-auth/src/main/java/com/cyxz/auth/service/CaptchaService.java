package com.cyxz.auth.service;

import java.util.Map;

/**
 * 验证码服务接口
 */
public interface CaptchaService {

    /**
     * 生成图形验证码
     * <p>使用 Hutool 生成 4 位线条验证码，Base64 返回前端，正确码值缓存至 Redis。
     *
     * @return uuid + Base64 图片
     */
    Map<String, String> generateCaptcha();
}
