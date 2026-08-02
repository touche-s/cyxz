package com.cyxz.common.web;

import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 解析 {@link AdminUser} 注解，校验 X-User-Role 请求头是否为 admin。
 * <p>非 admin 角色抛 {@link ErrorCode#FORBIDDEN}。
 */
public class AdminUserResolver implements HandlerMethodArgumentResolver {

    private static final String ROLE_HEADER = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AdminUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String role = webRequest.getHeader(ROLE_HEADER);
        if (!"admin".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行此操作");
        }
        return null;
    }
}
