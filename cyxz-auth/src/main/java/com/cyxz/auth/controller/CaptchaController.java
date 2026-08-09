package com.cyxz.auth.controller;

import com.cyxz.auth.service.CaptchaService;
import com.cyxz.common.base.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 验证码控制器
 * <p>仅做路由转发，业务逻辑下沉至 CaptchaService。
 */
@Tag(name = "验证码服务", description = "验证码控制器")
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
    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        return Result.success(captchaService.generateCaptcha());
    }
}
