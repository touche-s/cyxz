package com.cyxz.common.base;

/**
 * 资源不存在异常
 * <p>用于请求的资源（帖子、用户、评论等）在数据库中不存在时抛出。
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * 默认构造，消息取自 ErrorCode.NOT_FOUND
     */
    public ResourceNotFoundException() {
        super(ErrorCode.NOT_FOUND);
    }

    /**
     * 自定义消息构造
     *
     * @param message 错误信息
     */
    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    /**
     * 指定资源类型和 ID 构造
     *
     * @param resource 资源类型（如 "帖子"）
     * @param id       资源 ID
     */
    public ResourceNotFoundException(String resource, Long id) {
        super(ErrorCode.NOT_FOUND, resource + "不存在: " + id);
    }
}
