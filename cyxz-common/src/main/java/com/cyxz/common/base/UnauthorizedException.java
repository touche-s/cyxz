package com.cyxz.common.base;

/**
 * 未授权异常
 * <p>用于需要登录但未提供有效 Token 或 Token 已失效的场景。
 */
public class UnauthorizedException extends BusinessException {

    /**
     * 默认构造，消息取自 ErrorCode.UNAUTHORIZED
     */
    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 自定义消息构造
     *
     * @param message 错误信息
     */
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
