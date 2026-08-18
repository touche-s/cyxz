package com.cyxz.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 5, max = 11, message = "账号长度5-11位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20个字符")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String captcha;

    @NotBlank(message = "验证码UUID不能为空")
    private String captchaUuid;
}
