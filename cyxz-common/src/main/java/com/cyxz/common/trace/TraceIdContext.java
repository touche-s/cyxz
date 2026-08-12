package com.cyxz.common.trace;

/**
 * TraceId 上下文，基于 ThreadLocal 在当前请求/消息处理线程中传递 traceId。
 * <p>由 {@link TraceIdFilter} 在 HTTP 请求入口设置，
 * {@link com.cyxz.common.config.RabbitMqConfig} 在 MQ 发送时注入消息头，
 * 消费者从消息头提取后重新设置。
 */
public final class TraceIdContext {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private TraceIdContext() {}

    public static void set(String traceId) {
        CONTEXT.set(traceId);
    }

    public static String get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
