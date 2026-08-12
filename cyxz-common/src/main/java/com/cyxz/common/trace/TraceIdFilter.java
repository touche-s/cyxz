package com.cyxz.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 过滤器：从请求头读取或生成 traceId，注入 MDC 与 TraceIdContext。
 * <p>Gateway 已生成并注入 X-Trace-Id 时直接复用；未携带时自行生成（兼容本地调试）。
 * <p>执行顺序为最高优先级，确保后续所有日志均带 traceId。
 */
@Slf4j
@Component
@Order(Integer.MIN_VALUE)
@ConditionalOnWebApplication
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put("traceId", traceId);
        TraceIdContext.set(traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            TraceIdContext.clear();
        }
    }
}
