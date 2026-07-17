package com.cyxz.common.base;

/**
 * 业务异常基类
 * <p>所有自定义业务异常的父类，携带错误码和自定义消息，
 * 由 GlobalExceptionHandler 统一拦截并转换为 Result 响应。
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * 使用错误码构造，消息取自 ErrorCode
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码 + 自定义消息构造
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码 + 异常原因构造
     *
     * @param errorCode 错误码枚举
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMsg(), cause);
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码 + 自定义消息 + 异常原因构造
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public Integer getCode() {
        return code;
    }
}
