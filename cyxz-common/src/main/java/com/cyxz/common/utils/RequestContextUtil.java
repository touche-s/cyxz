package com.cyxz.common.utils;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.function.Supplier;

/**
 * 请求上下文工具
 * <p>将主线程的 {@link RequestAttributes} 传递到异步线程，解决 CompletableFuture
 * /线程池场景下 {@link RequestContextHolder} 取不到请求属性的问题（Feign 拦截器由此拿不到 X-User-Id）。
 */
public final class RequestContextUtil {

    private RequestContextUtil() {
    }

    /**
     * 包装 Supplier，使其在异步线程中能读到主线程的 RequestAttributes
     *
     * @param supplier 原始 Supplier
     * @param <T>      返回类型
     * @return 包装后的 Supplier
     */
    public static <T> Supplier<T> wrap(Supplier<T> supplier) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return () -> {
            RequestAttributes previous = RequestContextHolder.getRequestAttributes();
            try {
                if (attrs != null) {
                    RequestContextHolder.setRequestAttributes(attrs);
                }
                return supplier.get();
            } finally {
                // 恢复线程原有状态，避免线程池复用导致上下文污染
                if (previous == null) {
                    RequestContextHolder.resetRequestAttributes();
                } else {
                    RequestContextHolder.setRequestAttributes(previous);
                }
            }
        };
    }
}
