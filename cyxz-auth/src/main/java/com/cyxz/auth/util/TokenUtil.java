package com.cyxz.auth.util;

import org.springframework.util.StringUtils;

/**
 * Token 工具类
 * <p>提供 Bearer Token 解析等辅助方法。
 */
public final class TokenUtil {

    private static final String BEARER_PREFIX = "Bearer ";

    private TokenUtil() {
    }

    /**
     * 从 Authorization 请求头中提取 Token
     * <p>截取 "Bearer " 前缀后的 Token 字符串。
     *
     * @param authHeader Authorization 请求头原始值
     * @return Token 字符串，如果为空或不合法则返回 null
     */
    public static String extractBearerToken(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
