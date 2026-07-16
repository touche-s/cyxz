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

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

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
