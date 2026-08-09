package com.cyxz.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交注解
 * <p>贴在 Controller 方法上，AOP 切面在方法执行前以 {@code SET key 1 NX EX interval}
 * 的语义拦截 interval 秒内的重复请求（同一用户 + 同一接口 + 相同参数指纹）。
 * <p>key 生成规则：
 * <ul>
 *   <li>默认：{@code prevent:repeat:{userId}:{URI}:{参数hash}}，userId 从 X-User-Id 头取</li>
 *   <li>自定义：{@link #key()} 不为空时按 SpEL 表达式求值</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreventRepeat {

    /** 防重窗口，配合 {@link #unit()} 使用，默认 3 秒 */
    int interval() default 3;

    /** 防重窗口单位，默认秒 */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 自定义 key 的 SpEL 表达式，可引用方法参数（如 {@code #userId + ':' + #request.title}）。
     * 留空走默认规则（userId + URI + 参数 hash）。
     */
    String key() default "";

    /** 拦截时的提示消息，留空用 ErrorCode.REPEAT_SUBMIT 的默认消息 */
    String message() default "";
}
