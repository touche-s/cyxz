package com.cyxz.auth.service.impl;

import com.cyxz.common.constant.CacheKeyConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CaptchaServiceImpl 单元测试
 * <p>覆盖验证码生成与 Redis 缓存写入。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CaptchaServiceImpl 验证码服务")
class CaptchaServiceImplTest {

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks
    private CaptchaServiceImpl captchaService;

    @Test
    @DisplayName("生成验证码返回 uuid 和 image（均非空）")
    void shouldReturnUuidAndImage() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        Map<String, String> result = captchaService.generateCaptcha();

        assertNotNull(result.get("uuid"));
        assertFalse(result.get("uuid").isEmpty());
        assertNotNull(result.get("image"));
        assertFalse(result.get("image").isEmpty());
    }

    @Test
    @DisplayName("验证码存入 Redis：key=captchaKey、code 小写、TTL=5 分钟")
    void shouldStoreCaptchaInRedis() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        Map<String, String> result = captchaService.generateCaptcha();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(keyCaptor.capture(), codeCaptor.capture(),
                eq(CacheKeyConstants.CAPTCHA_EXPIRE_MINUTES), eq(TimeUnit.MINUTES));

        assertEquals(CacheKeyConstants.getCaptchaKey(result.get("uuid")), keyCaptor.getValue());
        String code = codeCaptor.getValue();
        assertNotNull(code);
        // 服务端存储时已 toLowerCase，校验存入 Redis 的码值确实为小写
        assertEquals(code.toLowerCase(), code);
    }
}
