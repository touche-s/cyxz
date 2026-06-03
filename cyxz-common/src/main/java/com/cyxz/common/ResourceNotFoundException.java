package com.cyxz.common;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(404, message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(404, resource + "不存在: " + id);
    }
}
