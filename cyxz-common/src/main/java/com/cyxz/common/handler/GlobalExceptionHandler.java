package com.cyxz.common.handler;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ResourceNotFoundException;
import com.cyxz.common.base.Result;
import com.cyxz.common.base.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Result<Void> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("资源不存在: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("未授权访问: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统异常，请稍后重试");
    }
}
