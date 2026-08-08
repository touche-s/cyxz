package com.cyxz.common.security;

/**
 * Token 剩余时间 ThreadLocal 上下文
 * <p>由 {@link HeaderAuthenticationFilter} 从 {@code X-Token-Remaining} 头读取并设置，
 * 供 {@link GlobalPermissionProviderImpl} 和 {@link CirclePermissionEvaluator} 回写 Redis 时对齐 TTL。
 * <p>请求结束后由 Filter 在 finally 中清除。
 */
public final class TokenTtlContext {

    private static final ThreadLocal<Long> REMAINING_SECONDS = new ThreadLocal<>();

    private TokenTtlContext() {
    }

    public static void set(long seconds) {
        REMAINING_SECONDS.set(seconds);
    }

    /**
     * 获取当前请求的 Token 剩余秒数
     *
     * @return 剩余秒数，未设置或为 0 时返回 0
     */
    public static long get() {
        Long val = REMAINING_SECONDS.get();
        return val != null ? val : 0;
    }

    public static void clear() {
        REMAINING_SECONDS.remove();
    }
}
