package com.cyxz.common.web;

import java.lang.annotation.*;

/**
 * 当前登录用户 ID 参数注解
 * <p>由 {@link CurrentUserResolver} 从网关注入的 {@code X-User-Id} 请求头解析。
 * 默认 required=true（缺失时抛 401），游客可访问的接口用 {@code @CurrentUser(required=false)}。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {

    /** 是否必须登录，true 时 header 缺失抛 UNAUTHORIZED */
    boolean required() default true;
}
