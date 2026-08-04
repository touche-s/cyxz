package com.cyxz.message.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.message.vo.NotificationVO;
import com.cyxz.message.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 * <p>提供通知列表查询、未读统计、已读标记等接口。
 */
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取通知列表
     * <p>支持按类型筛选，默认按创建时间倒序分页。
     *
     * @param userId 当前登录用户 ID
     * @param type   通知类型（可选）
     * @param page   页码
     * @param size   每页条数
     * @return 通知分页结果
     */
    @GetMapping("/notifications")
    public Result<PageResult<NotificationVO>> list(
            @CurrentUser Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(notificationService.list(userId, type, page, size));
    }

    /**
     * 获取未读数量
     *
     * @param userId 当前登录用户 ID
     * @return 未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount(@CurrentUser Long userId) {
        return Result.success(notificationService.unreadCount(userId));
    }

    /**
     * 标记单条已读
     *
     * @param userId 当前登录用户 ID
     * @param id     通知 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@CurrentUser Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return Result.success();
    }

    /**
     * 全部已读
     *
     * @param userId 当前登录用户 ID
     * @return 操作结果
     */
    @PutMapping("/read-all")
    public Result<Void> markAllRead(@CurrentUser Long userId) {
        notificationService.markAllRead(userId);
        return Result.success();
    }

}
