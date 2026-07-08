package com.cyxz.common.feign;

import com.cyxz.common.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 用户服务 Feign 客户端
 * <p>auth 服务注册后调用，初始化默认用户资料。
 * userId 通过请求头传递，不暴露在 URL 上。
 */
@FeignClient(name = "cyxz-user")
public interface UserFeignClient {

    /**
     * 创建默认用户资料
     *
     * @param userId 用户 ID（通过 X-User-Id 请求头传递）
     */
    @PostMapping("/user/internal/profile/init")
    Result<Void> initDefaultProfile(@RequestHeader("X-User-Id") Long userId);
}
