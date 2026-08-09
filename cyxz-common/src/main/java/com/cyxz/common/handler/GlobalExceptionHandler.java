package com.cyxz.common.handler;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>拦截业务异常、参数校验异常和系统异常，统一转换为 Result 响应返回。
 * <p>区分客户端错误（4xx）与服务端错误（5xx）：前者返回具体语义错误码便于前端处理，
 * 后者统一返回 SYSTEM_ERROR 避免泄漏内部细节。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理 @Valid 校验失败（@RequestBody 与表单参数）
     * <p>Spring 6 起 MethodArgumentNotValidException 继承自 BindException，统一处理即可。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 处理 @RequestParam / @PathVariable 校验失败（@Validated + @Min/@Size 等注解）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 请求体 JSON 格式错误或缺失
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return Result.fail(ErrorCode.REQUEST_BODY_INVALID.getCode(), ErrorCode.REQUEST_BODY_INVALID.getMsg());
    }

    /**
     * 路径变量类型不匹配（如 /post/abc 中 abc 非 Long）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: param={}, value={}", e.getName(), e.getValue());
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "参数 " + e.getName() + " 类型不匹配");
    }

    /**
     * 上传文件超过限制大小
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件过大: {}", e.getMessage());
        return Result.fail(ErrorCode.UPLOAD_TOO_LARGE.getCode(), ErrorCode.UPLOAD_TOO_LARGE.getMsg());
    }

    /**
     * 请求路径不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("路径不存在: {}", e.getRequestURL());
        return Result.fail(ErrorCode.PATH_NOT_FOUND.getCode(), ErrorCode.PATH_NOT_FOUND.getMsg());
    }

    /**
     * 兜底处理所有未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMsg());
    }
}
