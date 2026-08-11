package com.cyxz.common.handler;

import com.cyxz.common.annotation.PreventRepeat;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.CacheKeyConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面
 * <p>拦截所有标注 {@link PreventRepeat} 的方法，方法执行前执行 Lua 脚本：
 * SET key 1 NX EX interval。返回 1 表示首次（放行），返回 0 表示重复（拦截）。
 * <p>key 生成：
 * <ul>
 *   <li>默认：{@code prevent:repeat:{userId}:{URI}:{参数hash}}，userId 从 X-User-Id 头取，URI 即请求路径</li>
 *   <li>自定义：按注解 {@code key()} 的 SpEL 表达式求值</li>
 * </ul>
 * <p>异常容错：Redis 不可用时降级放行，避免防重组件故障导致业务不可用。
 */
@Aspect
@Component
public class PreventRepeatAspect {

    private static final Logger log = LoggerFactory.getLogger(PreventRepeatAspect.class);

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String DEFAULT_VALUE = "1";

    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<Long> script;

    public PreventRepeatAspect(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>();
        this.script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/preventRepeat.lua")));
        this.script.setResultType(Long.class);
    }

    @Around("@annotation(preventRepeat)")
    public Object around(ProceedingJoinPoint pjp, PreventRepeat preventRepeat) throws Throwable {
        String key = buildKey(pjp, preventRepeat);
        long ttlSeconds = preventRepeat.unit().toSeconds(preventRepeat.interval());

        boolean firstTime;
        try {
            Long ret = redisTemplate.execute(
                    script,
                    Collections.singletonList(key),
                    String.valueOf(ttlSeconds),
                    DEFAULT_VALUE);
            firstTime = ret != null && ret == 1L;
        } catch (Exception e) {
            // Redis 异常降级放行，防重组件故障不阻塞主流程
            log.warn("防重复提交检查失败，降级放行: key={}", key, e);
            return pjp.proceed();
        }

        if (!firstTime) {
            String msg = preventRepeat.message().isBlank()
                    ? ErrorCode.REPEAT_SUBMIT.getMsg()
                    : preventRepeat.message();
            log.debug("拦截重复提交: key={}", key);
            return Result.fail(ErrorCode.REPEAT_SUBMIT.getCode(), msg);
        }
        return pjp.proceed();
    }

    /**
     * 生成防重 key
     * <p>默认规则：prevent:repeat:{userId}:{URI}:{参数hash}
     * <p>自定义规则：注解 key() 不为空时直接使用（简化版，不做 SpEL，避免引入额外依赖与解析成本）
     */
    private String buildKey(ProceedingJoinPoint pjp, PreventRepeat anno) {
        if (!anno.key().isBlank()) {
            return CacheKeyConstants.PREVENT_REPEAT_PREFIX + anno.key();
        }
        HttpServletRequest request = currentRequest();
        String userId = "anonymous";
        String uri = "unknown";
        if (request != null) {
            userId = headerOr(request, USER_ID_HEADER, "anonymous");
            uri = request.getRequestURI();
        }
        String argsHash = argsHash(pjp);
        return CacheKeyConstants.PREVENT_REPEAT_PREFIX + userId + ":" + uri + ":" + argsHash;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String headerOr(HttpServletRequest req, String name, String fallback) {
        String v = req.getHeader(name);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    /**
     * 参数指纹：按参数名 + 值 hash，避免敏感参数明文进 key
     */
    private String argsHash(ProceedingJoinPoint pjp) {
        try {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            Method method = sig.getMethod();
            Parameter[] params = method.getParameters();
            Object[] args = pjp.getArgs();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < params.length; i++) {
                sb.append(params[i].getName()).append('=')
                  .append(args[i] == null ? "null" : args[i].toString()).append('|');
            }
            return md5Hex(sb.toString());
        } catch (Exception e) {
            // 反射失败兜底：用对象 hash，仍能区分大多数请求
            return Integer.toHexString(java.util.Arrays.hashCode(pjp.getArgs()));
        }
    }

    private String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 8);
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
