package com.cyxz.common;

/**
 * 资源不存在异常
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * 使用默认消息构造
     */
    public ResourceNotFoundException() {
        super(ErrorCode.NOT_FOUND);
    }

    /**
     * 使用自定义消息构造
     *
     * @param message 自定义错误信息
     */
    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    /**
     * 指定资源类型和ID构造
     *
     * @param resource 资源类型
     * @param id       资源ID
     */
    public ResourceNotFoundException(String resource, Long id) {
        super(ErrorCode.NOT_FOUND, resource + "不存在: " + id);
    }
}
