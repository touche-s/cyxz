package com.cyxz.message.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.message.dto.CreateNotificationRequest;
import com.cyxz.message.vo.NotificationVO;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 创建通知
     *
     * @param request 创建通知请求
     */
    void create(CreateNotificationRequest request);

    /**
     * 分页查询通知列表
     *
     * @param userId 接收者用户 ID
     * @param type   通知类型（可选）
     * @param page   页码
     * @param size   每页条数
     * @return 通知分页结果
     */
    PageResult<NotificationVO> list(Long userId, String type, int page, int size);

    /**
     * 查询未读通知数量
     *
     * @param userId 用户 ID
     * @return 未读数量
     */
    int unreadCount(Long userId);

    /**
     * 标记单条通知已读
     *
     * @param userId         用户 ID
     * @param notificationId 通知 ID
     */
    void markRead(Long userId, Long notificationId);

    /**
     * 标记全部通知已读
     *
     * @param userId 用户 ID
     */
    void markAllRead(Long userId);
}
