package com.cyxz.common.base;

/**
 * 资源不存在异常
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException() {
        super(ErrorCode.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(ErrorCode.NOT_FOUND, resource + "不存在: " + id);
    }
}
