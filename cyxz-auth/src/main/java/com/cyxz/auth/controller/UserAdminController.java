package com.cyxz.auth.controller;

import com.cyxz.auth.service.UserAdminService;
import com.cyxz.auth.vo.UserAdminVO;
import com.cyxz.common.base.Result;
import com.cyxz.common.web.AdminUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理（管理员端）
 */
@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping("/list")
    public Result<List<UserAdminVO>> list(@AdminUser Object admin) {
        return Result.success(userAdminService.listAll());
    }

    @PutMapping("/{id}/disable")
    public Result<Void> disable(@AdminUser Object admin, @PathVariable Long id) {
        userAdminService.disable(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    public Result<Void> enable(@AdminUser Object admin, @PathVariable Long id) {
        userAdminService.enable(id);
        return Result.success();
    }
}
