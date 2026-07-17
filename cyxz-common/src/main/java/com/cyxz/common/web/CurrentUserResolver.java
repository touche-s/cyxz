package com.cyxz.common.web;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 解析 {@link CurrentUser} 注解参数，从 {@code X-User-Id} 头取 Long 型用户 ID。
 * <p>required=true 且 header 缺失/非数字时抛 {@link ErrorCode#UNAUTHORIZED}；
 * required=false 且 header 缺失时返回 null（游客）。
 */
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * 判断参数是否支持解析：标注了 {@link CurrentUser} 且类型为 {@link Long}。
     *
     * @param parameter 方法参数
     * @return 是否支持
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    /**
     * 从请求头 X-User-Id 解析用户 ID。
     * <p>required=true 且 header 缺失/非数字时抛 {@link ErrorCode#UNAUTHORIZED}。
     *
     * @param parameter     方法参数
     * @param mavContainer  ModelAndView 容器
     * @param webRequest    当前请求
     * @param binderFactory 数据绑定工厂
     * @return 用户 ID，游客为 null
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        CurrentUser anno = parameter.getParameterAnnotation(CurrentUser.class);
        String header = webRequest.getHeader(USER_ID_HEADER);
        if (header == null || header.isEmpty()) {
            if (anno != null && anno.required()) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            return null;
        }
        try {
            return Long.valueOf(header);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
