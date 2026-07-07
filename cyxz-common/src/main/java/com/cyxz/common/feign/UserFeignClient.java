package com.cyxz.common.feign;

import com.cyxz.common.base.Result;
import com.cyxz.common.base.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 * <p>供其他服务通过 Feign 调用 cyxz-user 查询用户基础信息。
 */
@FeignClient(name = "cyxz-user", path = "/user")
public interface UserFeignClient {

    /**
     * 根据 ID 查询单个用户
     *
     * @param id 用户 ID
     * @return 用户基础信息
     */
    @GetMapping("/{id}")
    Result<UserVO> getUserById(@PathVariable("id") Long id);

    /**
     * 批量查询用户
     *
     * @param ids 用户 ID 列表
     * @return 用户基础信息列表
     */
    @GetMapping("/batch")
    Result<List<UserVO>> getUsersBatch(@RequestParam("ids") List<Long> ids);
}
