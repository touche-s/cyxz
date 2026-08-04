package com.cyxz.common.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * Feign 降级工厂抽象基类
 * <p>统一降级入口日志，子类只需提供服务名与各方法的安全默认值。
 *
 * @param <T> Feign 客户端类型
 */
public abstract class AbstractFeignFallbackFactory<T> implements FallbackFactory<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public final T create(Throwable cause) {
        log.warn("{} 调用降级: {}", serviceName(), cause.getMessage());
        return createFallback(cause);
    }

    /**
     * 返回服务名，用于统一降级日志
     */
    protected abstract String serviceName();

    /**
     * 创建降级实例，各方法返回安全默认值
     */
    protected abstract T createFallback(Throwable cause);
}
