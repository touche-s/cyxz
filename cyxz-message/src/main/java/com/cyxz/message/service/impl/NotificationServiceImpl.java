package com.cyxz.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.message.api.dto.CreateNotificationRequest;
import com.cyxz.message.api.vo.NotificationVO;
import com.cyxz.message.entity.NotificationPO;
import com.cyxz.message.mapper.NotificationMapper;
import com.cyxz.message.service.NotificationService;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.utils.UserFeignHelper;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 * <p>管理通知的创建、查询、已读标记等操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserFeignClient userFeignClient;

    /**
     * 创建通知
     * <p>不给自己发通知（receiverId == senderId 时直接返回）。
     * <p>去重：同 receiver + 同 sender + 同 type + 同 target 的通知已存在时跳过，防止取关重关等操作刷通知。
     * 内容超过 200 字自动截断。
     */
    @Override
    public void create(CreateNotificationRequest request) {
        if (request.getReceiverId().equals(request.getSenderId())) {
            return; // 不给自己发通知
        }
        // 去重：同一发送者对同一目标同类型的通知已存在则跳过
        LambdaQueryWrapper<NotificationPO> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(NotificationPO::getReceiverId, request.getReceiverId())
                .eq(NotificationPO::getSenderId, request.getSenderId())
                .eq(NotificationPO::getType, request.getType())
                .eq(request.getTargetId() != null, NotificationPO::getTargetId, request.getTargetId());
        if (notificationMapper.selectCount(dupWrapper) > 0) {
            log.debug("通知去重跳过: type={}, receiverId={}, senderId={}", request.getType(), request.getReceiverId(), request.getSenderId());
            return;
        }
        NotificationPO po = new NotificationPO();
        po.setReceiverId(request.getReceiverId());
        po.setSenderId(request.getSenderId());
        po.setType(request.getType());
        po.setTargetId(request.getTargetId());
        po.setTargetType(request.getTargetType());
        po.setRelatedId(request.getRelatedId());
        po.setContent(request.getContent() != null && request.getContent().length() > 200
                ? request.getContent().substring(0, 200) : request.getContent());
        po.setIsRead(0);
        notificationMapper.insert(po);
        log.debug("创建通知: type={}, receiverId={}, senderId={}", request.getType(), request.getReceiverId(), request.getSenderId());
    }

    /**
     * 分页查询通知列表
     * <p>按接收者筛选，支持按类型过滤，按创建时间倒序排列。
     */
    @Override
    public PageResult<NotificationVO> list(Long userId, String type, int page, int size) {
        LambdaQueryWrapper<NotificationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationPO::getReceiverId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(NotificationPO::getType, type);
        }
        wrapper.orderByDesc(NotificationPO::getCreateTime);
        Page<NotificationPO> pageResult = notificationMapper.selectPage(PageConstants.pageOf(page, size), wrapper);

        List<NotificationPO> records = pageResult.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(page, size);
        }

        // 批量查询发送者用户信息，填充头像和昵称
        Set<Long> senderIds = records.stream().map(NotificationPO::getSenderId).collect(Collectors.toSet());
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, senderIds);

        List<NotificationVO> vos = records.stream().map(po -> toVO(po, userMap)).collect(Collectors.toList());
        return PageResult.of(vos, pageResult.getTotal(), page, size);
    }

    /**
     * 查询未读通知数量
     */
    @Override
    public int unreadCount(Long userId) {
        LambdaQueryWrapper<NotificationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationPO::getReceiverId, userId)
                .eq(NotificationPO::getIsRead, 0);
        return notificationMapper.selectCount(wrapper).intValue();
    }

    /**
     * 标记单条通知已读
     */
    @Override
    public void markRead(Long userId, Long notificationId) {
        LambdaUpdateWrapper<NotificationPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NotificationPO::getReceiverId, userId)
                .eq(NotificationPO::getId, notificationId)
                .set(NotificationPO::getIsRead, 1);
        notificationMapper.update(wrapper);
    }

    /**
     * 标记全部通知已读
     */
    @Override
    public void markAllRead(Long userId) {
        LambdaUpdateWrapper<NotificationPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NotificationPO::getReceiverId, userId)
                .eq(NotificationPO::getIsRead, 0)
                .set(NotificationPO::getIsRead, 1);
        notificationMapper.update(wrapper);
    }

    /** 将实体转换为 VO，并从用户信息映射中填充头昵称和头像 */
    private NotificationVO toVO(NotificationPO po, Map<Long, UserProfileVO> userMap) {
        NotificationVO vo = new NotificationVO();
        vo.setId(po.getId());
        vo.setSenderId(po.getSenderId());
        vo.setType(po.getType());
        vo.setTargetId(po.getTargetId());
        vo.setTargetType(po.getTargetType());
        vo.setContent(po.getContent());
        vo.setIsRead(po.getIsRead() != null && po.getIsRead() == 1);
        vo.setCreateTime(po.getCreateTime());

        UserProfileVO user = userMap.get(po.getSenderId());
        if (user != null) {
            vo.setSenderName(user.getNickname());
            vo.setSenderAvatar(user.getAvatar());
        }
        return vo;
    }
}
