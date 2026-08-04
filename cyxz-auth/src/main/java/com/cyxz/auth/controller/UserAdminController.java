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

    /**
     * 查询全部用户列表
     * @param admin 当前登录的管理员
     * @return 用户管理视图对象列表
     */
    @GetMapping("/list")
    public Result<List<UserAdminVO>> list(@AdminUser Object admin) {
        return Result.success(userAdminService.listAll());
    }

    /**
     * 禁用指定用户
     * @param admin 当前登录的管理员
     * @param id 被禁用用户的唯一标识
     * @return 无业务数据的统一响应
     */
    @PutMapping("/{id}/disable")
    public Result<Void> disable(@AdminUser Object admin, @PathVariable Long id) {
        userAdminService.disable(id);
        return Result.success();
    }

    /**
     * 启用指定用户
     * @param admin 当前登录的管理员
     * @param id 被启用用户的唯一标识
     * @return 无业务数据的统一响应
     */
    @PutMapping("/{id}/enable")
    public Result<Void> enable(@AdminUser Object admin, @PathVariable Long id) {
        userAdminService.enable(id);
        return Result.success();
    }
}
