package com.cyxz.common.web;

import java.lang.annotation.*;

/**
 * 管理员角色校验注解
 * <p>由 {@link AdminUserResolver} 从网关传入的 {@code X-User-Role} 请求头校验。
 * 非 admin 角色抛出 {@link com.cyxz.common.base.ErrorCode#FORBIDDEN}。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminUser {
}
