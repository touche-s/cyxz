package com.cyxz.common.feign;

import com.cyxz.common.base.Result;
import com.cyxz.common.base.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "cyxz-user", path = "/user")
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<UserVO> getUserById(@PathVariable("id") Long id);

    @GetMapping("/batch")
    Result<List<UserVO>> getUsersBatch(@RequestParam("ids") List<Long> ids);
}
