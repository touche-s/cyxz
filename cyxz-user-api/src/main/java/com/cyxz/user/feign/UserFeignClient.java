package com.cyxz.user.feign;

import com.cyxz.common.base.Result;
import com.cyxz.user.vo.UserProfileVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 用户服务 Feign 客户端
 * <p>供其他微服务调用 cyxz-user 的接口。
 */
@FeignClient(name = "cyxz-user")
public interface UserFeignClient {

    /**
     * 创建默认用户资料
     *
     * @param userId   用户 ID
     * @param username 用户名
     */
    @PostMapping("/user/internal/profile/init/{userId}/{username}")
    Result<Void> initDefaultProfile(@PathVariable("userId") Long userId, @PathVariable("username") String username);

    /**
     * 根据用户 ID 查询资料
     *
     * @param userId 用户 ID
     * @return 用户资料
     */
    @GetMapping("/user/{userId}")
    Result<UserProfileVO> getById(@PathVariable("userId") Long userId);
}
