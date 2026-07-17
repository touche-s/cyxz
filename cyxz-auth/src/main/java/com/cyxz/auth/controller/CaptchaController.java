package com.cyxz.auth.controller;

import com.cyxz.auth.service.CaptchaService;
import com.cyxz.common.base.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 验证码控制器
 * <p>仅做路由转发，业务逻辑下沉至 CaptchaService。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 获取图形验证码
     *
     * @return uuid + Base64 图片
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        return Result.success(captchaService.generateCaptcha());
    }
}
