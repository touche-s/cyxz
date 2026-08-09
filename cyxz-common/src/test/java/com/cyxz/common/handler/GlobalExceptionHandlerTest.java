package com.cyxz.common.handler;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 单元测试
 * <p>覆盖全部 18 个 @ExceptionHandler 方法，验证异常到错误码的映射正确性。
 * <p>回归价值：后续新增/调整错误码时，本测试确保异常处理映射不漏不错。
 */
@DisplayName("GlobalExceptionHandler 全局异常处理")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** 断言返回 Result 的 code 与 message 与给定 ErrorCode 一致 */
    private void assertResult(Result<?> result, ErrorCode expected) {
        assertEquals(expected.getCode(), result.getCode());
        assertEquals(expected.getMsg(), result.getMessage());
    }

    // ==================== 业务异常 ====================

    @Nested
    @DisplayName("业务异常")
    class Business {

        @Test
        @DisplayName("BusinessException 透传 code + message")
        void shouldPassthroughBusinessException() {
            BusinessException e = new BusinessException(ErrorCode.POST_NOT_FOUND);
            Result<Void> result = handler.handleBusinessException(e);
            assertEquals(ErrorCode.POST_NOT_FOUND.getCode(), result.getCode());
            assertEquals(ErrorCode.POST_NOT_FOUND.getMsg(), result.getMessage());
        }

        @Test
        @DisplayName("BusinessException 自定义消息透传")
        void shouldPassthroughCustomMessage() {
            BusinessException e = new BusinessException(ErrorCode.PARAM_ERROR, "标题不能为空");
            Result<Void> result = handler.handleBusinessException(e);
            assertEquals(ErrorCode.PARAM_ERROR.getCode(), result.getCode());
            assertEquals("标题不能为空", result.getMessage());
        }
    }

    // ==================== 参数校验异常 ====================

    @Nested
    @DisplayName("参数校验异常")
    class Validation {

        @Test
        @DisplayName("BindException 聚合字段错误，返回 PARAM_ERROR")
        void shouldAggregateFieldErrorsOnBindException() {
            BindException e = new BindException(new Object(), "target");
            e.addError(new FieldError("target", "name", "不能为空"));
            e.addError(new FieldError("target", "age", "必须大于0"));

            Result<Void> result = handler.handleBind(e);

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), result.getCode());
            assertEquals("name: 不能为空, age: 必须大于0", result.getMessage());
        }

        @Test
        @DisplayName("ConstraintViolationException 聚合违例，返回 PARAM_ERROR")
        void shouldAggregateViolationsOnConstraintViolation() {
            @SuppressWarnings("unchecked")
            ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
            jakarta.validation.Path path = mock(jakarta.validation.Path.class);
            when(path.toString()).thenReturn("name");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("不能为空");
            ConstraintViolationException e = new ConstraintViolationException(Collections.singleton(violation));

            Result<Void> result = handler.handleConstraintViolation(e);

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), result.getCode());
            assertNotNull(result.getMessage());
        }

        @Test
        @DisplayName("MethodArgumentTypeMismatchException 返回 PARAM_ERROR + 参数名")
        void shouldReturnTypeMismatchError() {
            MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException(
                    "abc", Long.class, "postId", mock(MethodParameter.class),
                    new IllegalArgumentException("For input string: \"abc\""));

            Result<Void> result = handler.handleTypeMismatch(e);

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), result.getCode());
            assertEquals("参数 postId 类型不匹配", result.getMessage());
        }

        @Test
        @DisplayName("MissingServletRequestParameterException 返回 PARAM_MISSING + 参数名")
        void shouldReturnMissingParamError() {
            MissingServletRequestParameterException e =
                    new MissingServletRequestParameterException("postId", "Long");

            Result<Void> result = handler.handleMissingParam(e);

            assertEquals(ErrorCode.PARAM_MISSING.getCode(), result.getCode());
            assertEquals("缺少必填参数: postId", result.getMessage());
        }

        @Test
        @DisplayName("MissingRequestHeaderException 返回 PARAM_MISSING + 请求头名")
        void shouldReturnMissingHeaderError() {
            MissingRequestHeaderException e =
                    new MissingRequestHeaderException("X-User-Id", mock(MethodParameter.class));

            Result<Void> result = handler.handleMissingHeader(e);

            assertEquals(ErrorCode.PARAM_MISSING.getCode(), result.getCode());
            assertEquals("缺少必填请求头: X-User-Id", result.getMessage());
        }

        @Test
        @DisplayName("MissingServletRequestPartException 返回 PARAM_MISSING + 部件名")
        void shouldReturnMissingPartError() {
            MissingServletRequestPartException e = new MissingServletRequestPartException("file");

            Result<Void> result = handler.handleMissingPart(e);

            assertEquals(ErrorCode.PARAM_MISSING.getCode(), result.getCode());
            assertEquals("缺少文件部分: file", result.getMessage());
        }

        @Test
        @DisplayName("HttpMessageNotReadableException 返回 REQUEST_BODY_INVALID")
        void shouldReturnRequestBodyInvalid() {
            HttpMessageNotReadableException e = new HttpMessageNotReadableException(
                    "JSON parse error", mock(org.springframework.http.HttpInputMessage.class));

            Result<Void> result = handler.handleHttpMessageNotReadable(e);

            assertResult(result, ErrorCode.REQUEST_BODY_INVALID);
        }
    }

    // ==================== 安全异常 ====================

    @Nested
    @DisplayName("安全认证异常")
    class Security {

        @Test
        @DisplayName("AccessDeniedException 返回 FORBIDDEN")
        void shouldReturnForbiddenOnAccessDenied() {
            AccessDeniedException e = new AccessDeniedException("无权访问");

            Result<Void> result = handler.handleAccessDenied(e);

            assertResult(result, ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("AuthenticationException 返回 UNAUTHORIZED")
        void shouldReturnUnauthorizedOnAuthException() {
            BadCredentialsException e = new BadCredentialsException("凭据错误");

            Result<Void> result = handler.handleAuthentication(e);

            assertResult(result, ErrorCode.UNAUTHORIZED);
        }
    }

    // ==================== HTTP 协议异常 ====================

    @Nested
    @DisplayName("HTTP 协议异常")
    class HttpProtocol {

        @Test
        @DisplayName("HttpRequestMethodNotSupportedException 返回 METHOD_NOT_ALLOWED")
        void shouldReturnMethodNotAllowed() {
            HttpRequestMethodNotSupportedException e =
                    new HttpRequestMethodNotSupportedException("GET");

            Result<Void> result = handler.handleMethodNotSupported(e);

            assertResult(result, ErrorCode.METHOD_NOT_ALLOWED);
        }

        @Test
        @DisplayName("HttpMediaTypeNotSupportedException 返回 UNSUPPORTED_MEDIA_TYPE")
        void shouldReturnUnsupportedMediaType() {
            HttpMediaTypeNotSupportedException e = new HttpMediaTypeNotSupportedException(
                    org.springframework.http.MediaType.APPLICATION_JSON,
                    java.util.List.of(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM));

            Result<Void> result = handler.handleMediaTypeNotSupported(e);

            assertResult(result, ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }

        @Test
        @DisplayName("NoHandlerFoundException 返回 PATH_NOT_FOUND")
        void shouldReturnPathNotFound() {
            NoHandlerFoundException e = new NoHandlerFoundException(
                    "GET", "/api/unknown", mock(org.springframework.http.HttpHeaders.class));

            Result<Void> result = handler.handleNoHandlerFound(e);

            assertResult(result, ErrorCode.PATH_NOT_FOUND);
        }
    }

    // ==================== 资源冲突 / 状态异常 ====================

    @Nested
    @DisplayName("资源冲突 / 状态异常")
    class Conflict {

        @Test
        @DisplayName("OptimisticLockingFailureException 返回 OPTIMISTIC_LOCK_CONFLICT")
        void shouldReturnOptimisticLockConflict() {
            OptimisticLockingFailureException e = new OptimisticLockingFailureException("版本冲突");

            Result<Void> result = handler.handleOptimisticLock(e);

            assertResult(result, ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }

        @Test
        @DisplayName("AsyncRequestTimeoutException 返回 SERVICE_UNAVAILABLE")
        void shouldReturnServiceUnavailableOnAsyncTimeout() {
            AsyncRequestTimeoutException e = new AsyncRequestTimeoutException();

            Result<Void> result = handler.handleAsyncTimeout(e);

            assertResult(result, ErrorCode.SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("MaxUploadSizeExceededException 返回 UPLOAD_TOO_LARGE")
        void shouldReturnUploadTooLarge() {
            MaxUploadSizeExceededException e = new MaxUploadSizeExceededException(10485760L);

            Result<Void> result = handler.handleMaxUploadSize(e);

            assertResult(result, ErrorCode.UPLOAD_TOO_LARGE);
        }
    }

    // ==================== 兜底异常 ====================

    @Nested
    @DisplayName("兜底异常")
    class Fallback {

        @Test
        @DisplayName("IllegalArgumentException 返回 PARAM_ERROR + 异常消息")
        void shouldReturnParamErrorOnIllegalArgument() {
            IllegalArgumentException e = new IllegalArgumentException("非法 token");

            Result<Void> result = handler.handleIllegalArgument(e);

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), result.getCode());
            assertEquals("非法 token", result.getMessage());
        }

        @Test
        @DisplayName("未知 Exception 返回 SYSTEM_ERROR，不泄漏内部细节")
        void shouldReturnSystemErrorOnUnknownException() {
            Exception e = new RuntimeException("NPE at internal service");

            Result<Void> result = handler.handleException(e);

            assertResult(result, ErrorCode.SYSTEM_ERROR);
        }
    }
}
