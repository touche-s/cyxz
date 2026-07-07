package com.cyxz.common.base;

/**
 * 业务异常基类
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMsg(), cause);
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
