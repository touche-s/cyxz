package com.cyxz.common.base;

import lombok.Getter;

/**
 * 错误码枚举
 * <p>统一定义系统中所有错误码及默认提示信息
 */
@Getter
public enum ErrorCode {

    // === 通用 ===
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    SYSTEM_ERROR(500, "系统异常"),

    // === 认证授权 ===
    UNAUTHORIZED(401, "无权操作"),
    TOKEN_EXPIRED(401, "登录已过期"),
    TOKEN_INVALID(401, "Token无效"),
    FORBIDDEN(403, "没有权限"),

    // === 参数校验 ===
    PARAM_ERROR(400, "参数错误"),
    PARAM_MISSING(400, "缺少必填参数"),

    // === 资源 ===
    NOT_FOUND(404, "资源不存在"),
    ALREADY_EXISTS(409, "已存在"),

    // === 业务 ===
    USER_NOT_FOUND(404, "用户不存在"),
    USER_DISABLED(403, "账号已被禁用"),
    USERNAME_EXISTS(409, "账号已存在"),
    POST_NOT_FOUND(404, "帖子不存在"),
    COMMENT_NOT_FOUND(404, "评论不存在"),
    CAPTCHA_ERROR(400, "验证码错误"),
    CAPTCHA_EXPIRED(400, "验证码已过期"),
    PASSWORD_ERROR(401, "密码错误"),
    ;

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
