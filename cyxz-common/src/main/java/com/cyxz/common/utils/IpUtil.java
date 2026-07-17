package com.cyxz.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * IP 工具类
 * <p>从 HttpServletRequest 中提取客户端真实 IP，优先读取反向代理透传的头。
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private IpUtil() {
    }

    /**
     * 获取客户端真实 IP
     * <p>X-Forwarded-For 可能是逗号分隔的多级代理链，取第一个非 unknown 的值。
     *
     * @param request HTTP 请求
     * @return 客户端 IP，获取不到返回 unknown
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
                // 多级代理时取第一个
                int comma = ip.indexOf(',');
                return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
            }
        }
        String ip = request.getRemoteAddr();
        return ip != null ? ip : UNKNOWN;
    }
}
