package com.cyxz.common.feign;

import com.cyxz.common.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 用户服务 Feign 客户端
 * <p>auth 服务注册后调用，初始化默认用户资料。
 */
@FeignClient(name = "cyxz-user")
public interface UserFeignClient {

    /**
     * 创建默认用户资料
     *
     * @param userId 用户 ID
     * @param username 用户名
     */
    @PostMapping("/user/internal/profile/init/{userId}/{username}")
    Result<Void> initDefaultProfile(@PathVariable("userId") Long userId, @PathVariable("username") String username);
}
