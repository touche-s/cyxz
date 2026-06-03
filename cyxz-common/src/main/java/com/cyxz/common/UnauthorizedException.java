package com.cyxz.common;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(401, message);
    }
}
