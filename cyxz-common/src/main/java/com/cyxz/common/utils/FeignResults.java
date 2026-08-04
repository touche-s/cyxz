package com.cyxz.common.utils;

import com.cyxz.common.base.Result;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Feign Result 解包工具
 * <p>统一处理 {@link Result} 在降级/异常时 data 为 null 的情况，消除散落各处的手写 null 判断。
 */
public final class FeignResults {

    private FeignResults() {}

    /**
     * 解包 Result，result 或 data 为 null 时返回 defaultValue
     */
    public static <T> T unwrapOr(Result<T> result, T defaultValue) {
        return result != null && result.getData() != null ? result.getData() : defaultValue;
    }

    /**
     * 解包 Result，result 或 data 为 null 时返回 null
     */
    public static <T> T unwrapOrNull(Result<T> result) {
        return result != null ? result.getData() : null;
    }

    /**
     * 解包 List，result 或 data 为 null 时返回空列表
     */
    public static <T> List<T> unwrapOrEmpty(Result<List<T>> result) {
        return result != null && result.getData() != null ? result.getData() : Collections.emptyList();
    }

    /**
     * 解包 Map，result 或 data 为 null 时返回空 Map
     */
    public static <K, V> Map<K, V> unwrapOrEmptyMap(Result<Map<K, V>> result) {
        return result != null && result.getData() != null ? result.getData() : Collections.emptyMap();
    }
}
