package com.cyxz.common;

/**
 * 未授权异常
 */
public class UnauthorizedException extends BusinessException {

    /**
     * 使用默认消息构造
     */
    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 使用自定义消息构造
     *
     * @param message 自定义错误信息
     */
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
