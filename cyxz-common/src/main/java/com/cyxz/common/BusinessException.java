package com.cyxz.common;

/**
 * 业务异常基类
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * 使用错误码构造
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码构造（自定义消息）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码构造（带异常原因）
     *
     * @param errorCode 错误码枚举
     * @param cause     异常原因
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMsg(), cause);
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码构造（自定义消息 + 异常原因）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @param cause     异常原因
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
