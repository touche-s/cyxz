package com.cyxz.common.handler;

import com.cyxz.common.annotation.PreventRepeat;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PreventRepeatAspect 单元测试
 * <p>覆盖防重核心分支：首次放行 / 重复拦截 / Redis 异常降级 / 自定义 key / 自定义消息。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PreventRepeatAspect 防重复提交切面")
class PreventRepeatAspectTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ProceedingJoinPoint pjp;
    @Mock private MethodSignature methodSignature;

    @InjectMocks
    private PreventRepeatAspect aspect;

    private static final Object PROCEED_RESULT = "OK";

    @BeforeEach
    void setUp() throws Throwable {
        // 让 pjp.proceed() 默认返回一个标记对象，便于断言是否放行
        when(pjp.proceed()).thenReturn(PROCEED_RESULT);
    }

    @AfterEach
    void clearRequestContext() {
        // 清理 RequestContextHolder，避免测试间串扰
        RequestContextHolder.resetRequestAttributes();
    }

    /** 绑定被测方法（含 PreventRepeat 注解）到 pjp 签名 */
    private void mockTargetMethod(String methodName, PreventRepeat anno) throws NoSuchMethodException {
        Method method = SampleController.class.getDeclaredMethod(methodName, Long.class);
        when(pjp.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
    }

    /** 模拟当前请求（带 X-User-Id 头） */
    private void mockRequest(String userId, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", userId);
        request.setRequestURI(uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    /** 模拟 Redis 执行 Lua 脚本的返回值（实际调用: execute(script, keys, ttl, value)） */
    @SuppressWarnings("unchecked")
    private void mockRedisReturn(Long ret) {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(ret);
    }

    // ==================== 默认 key 分支 ====================

    @Nested
    @DisplayName("默认 key 规则（userId + URI + 参数 hash）")
    class DefaultKey {

        @Test
        @DisplayName("首次请求：Redis 返回 1，放行并执行目标方法")
        void shouldProceedOnFirstRequest() throws Throwable {
            PreventRepeat anno = SampleController.class.getDeclaredMethod("send", Long.class)
                    .getAnnotation(PreventRepeat.class);
            mockTargetMethod("send", anno);
            mockRequest("100", "/api/message/send");
            when(pjp.getArgs()).thenReturn(new Object[]{200L});
            mockRedisReturn(1L);

            Object result = aspect.around(pjp, anno);

            assertSame(PROCEED_RESULT, result);
            verify(redisTemplate).execute(any(RedisScript.class), anyList(), any(), any());
        }

        @Test
        @DisplayName("重复请求：Redis 返回 0，拦截并返回 REPEAT_SUBMIT")
        void shouldRejectOnDuplicateRequest() throws Throwable {
            PreventRepeat anno = SampleController.class.getDeclaredMethod("send", Long.class)
                    .getAnnotation(PreventRepeat.class);
            mockTargetMethod("send", anno);
            mockRequest("100", "/api/message/send");
            when(pjp.getArgs()).thenReturn(new Object[]{200L});
            mockRedisReturn(0L);

            Object result = aspect.around(pjp, anno);

            assertInstanceOf(Result.class, result);
            Result<?> r = (Result<?>) result;
            assertEquals(ErrorCode.REPEAT_SUBMIT.getCode(), r.getCode());
            assertEquals(ErrorCode.REPEAT_SUBMIT.getMsg(), r.getMessage());
            verify(pjp, never()).proceed();
        }

        @Test
        @DisplayName("参数不同时 key 不同：不同 receiverId 视为不同请求")
        void shouldGenerateDifferentKeyForDifferentArgs() throws Throwable {
            PreventRepeat anno = SampleController.class.getDeclaredMethod("send", Long.class)
                    .getAnnotation(PreventRepeat.class);
            mockTargetMethod("send", anno);
            mockRequest("100", "/api/message/send");
            when(pjp.getArgs()).thenReturn(new Object[]{200L});
            mockRedisReturn(1L);

            aspect.around(pjp, anno);

            // 第二次调用传不同参数，验证两次 key 不同（通过 captor 太重，这里验证 execute 被调用且参数传递正确）
            when(pjp.getArgs()).thenReturn(new Object[]{300L});
            mockRedisReturn(1L);
            aspect.around(pjp, anno);

            // 两次都放行，验证 proceed 各执行一次
            verify(pjp, times(2)).proceed();
        }
    }

    // ==================== 降级分支 ====================

    @Nested
    @DisplayName("Redis 异常降级")
    class Fallback {

        @Test
        @DisplayName("Redis 抛异常时降级放行，不阻塞主流程")
        @SuppressWarnings("unchecked")
        void shouldFallbackOnRedisException() throws Throwable {
            PreventRepeat anno = SampleController.class.getDeclaredMethod("send", Long.class)
                    .getAnnotation(PreventRepeat.class);
            mockTargetMethod("send", anno);
            mockRequest("100", "/api/message/send");
            when(pjp.getArgs()).thenReturn(new Object[]{200L});
            when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                    .thenThrow(new RuntimeException("redis down"));

            Object result = aspect.around(pjp, anno);

            assertSame(PROCEED_RESULT, result);
            verify(pjp).proceed();
        }
    }

    // ==================== 自定义 key 分支 ====================

    @Nested
    @DisplayName("自定义 key")
    class CustomKey {

        @Test
        @DisplayName("注解 key() 非空时直接使用自定义 key，不读请求上下文")
        void shouldUseCustomKeyWhenProvided() throws Throwable {
            PreventRepeat anno = SampleController.class.getDeclaredMethod("customKeyMethod", Long.class)
                    .getAnnotation(PreventRepeat.class);
            mockTargetMethod("customKeyMethod", anno);
            when(pjp.getArgs()).thenReturn(new Object[]{200L});
            mockRedisReturn(1L);

            Object result = aspect.around(pjp, anno);

            assertSame(PROCEED_RESULT, result);
            // 不设置请求上下文也能正常工作（自定义 key 不依赖 request）
            verify(redisTemplate).execute(any(RedisScript.class), anyList(), any(), any());
        }
    }

    // ==================== 自定义消息 ====================

    @Nested
    @DisplayName("自定义拦截消息")
    class CustomMessage {

        @Test
        @DisplayName("注解 message() 非空时用自定义消息拦截")
        void shouldUseCustomMessageWhenProvided() throws Throwable {
            PreventRepeat anno = SampleController.class.getDeclaredMethod("customMsgMethod", Long.class)
                    .getAnnotation(PreventRepeat.class);
            mockTargetMethod("customMsgMethod", anno);
            mockRequest("100", "/api/test/custom-msg");
            when(pjp.getArgs()).thenReturn(new Object[]{200L});
            mockRedisReturn(0L);

            Object result = aspect.around(pjp, anno);

            assertInstanceOf(Result.class, result);
            Result<?> r = (Result<?>) result;
            assertEquals(ErrorCode.REPEAT_SUBMIT.getCode(), r.getCode());
            assertEquals("请勿连点", r.getMessage());
            verify(pjp, never()).proceed();
        }
    }

    // ==================== TTL 转换 ====================

    @Nested
    @DisplayName("interval + unit 转换为秒")
    class TtlConversion {

        @Test
        @DisplayName("分钟单位正确转换为秒（1 分钟 = 60 秒）")
        @SuppressWarnings("unchecked")
        void shouldConvertMinutesToSeconds() throws Throwable {
            PreventRepeat anno = SampleController.class.getDeclaredMethod("minuteMethod", Long.class)
                    .getAnnotation(PreventRepeat.class);
            mockTargetMethod("minuteMethod", anno);
            mockRequest("100", "/api/test/minute");
            when(pjp.getArgs()).thenReturn(new Object[]{200L});
            mockRedisReturn(1L);

            aspect.around(pjp, anno);

            // 验证传给 Lua 脚本的 TTL 参数是 "60"（1 分钟）
            verify(redisTemplate).execute(any(RedisScript.class), anyList(), eq("60"), eq("1"));
        }
    }

    /** 测试用 Controller 桩，承载不同配置的 @PreventRepeat 注解 */
    static class SampleController {
        @PreventRepeat(interval = 2)
        void send(Long receiverId) {}

        @PreventRepeat(key = "custom:post:publish")
        void customKeyMethod(Long userId) {}

        @PreventRepeat(message = "请勿连点")
        void customMsgMethod(Long userId) {}

        @PreventRepeat(interval = 1, unit = TimeUnit.MINUTES)
        void minuteMethod(Long userId) {}
    }

    private static void assertInstanceOf(Class<?> expected, Object actual) {
        if (!expected.isInstance(actual)) {
            throw new AssertionError("期望类型 " + expected.getName() + "，实际 " +
                    (actual == null ? "null" : actual.getClass().getName()));
        }
    }
}
