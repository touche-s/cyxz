package com.cyxz.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.utils.StatusUpdateHelper;
import com.cyxz.message.api.dto.CreateNotificationRequest;
import com.cyxz.message.api.enums.NotificationType;
import com.cyxz.message.api.event.NotificationEvent;
import com.cyxz.message.api.feign.MessageFeignClient;
import com.cyxz.user.entity.UserFollowPO;
import com.cyxz.user.mapper.UserFollowMapper;
import com.cyxz.user.service.FollowService;
import com.cyxz.user.vo.FollowUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关注服务实现
 * <p>管理 user_follow 表的关注/取关、统计与列表查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowMapper followMapper;
    private final MessageFeignClient messageFeignClient;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 关注目标用户（幂等，并发安全）
     * <p>目标：设为 status=1。并发安全策略：
     * <ol>
     *   <li>不存在记录 → 尝试插入，冲突时捕获 DuplicateKeyException 重查真实状态</li>
     *   <li>存在且 status=0 → 条件更新为 1</li>
     *   <li>存在且 status=1 → 幂等忽略</li>
     * </ol>
     * 唯一索引 uk_user_follow 作为数据库最终兜底。
     * 不允许关注自己。
     *
     * @param userId       当前登录用户 ID
     * @param targetUserId 目标用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能关注自己");
        }

        UserFollowPO exist = queryFollow(userId, targetUserId);

        if (exist == null) {
            try {
                UserFollowPO newFollow = new UserFollowPO();
                newFollow.setUserId(userId);
                newFollow.setFollowUserId(targetUserId);
                newFollow.setStatus(CommonStatus.ACTIVE);
                followMapper.insert(newFollow);
                log.info("关注用户: userId={}, followUserId={}", userId, targetUserId);
                // 发送关注通知 — MQ 异步
                try {
                    rabbitTemplate.convertAndSend(
                        "cyxz.notification.exchange",
                        "notification.create",
                        NotificationEvent.builder()
                            .receiverId(targetUserId)
                            .senderId(userId)
                            .type(NotificationType.USER_FOLLOWED.name())
                            .title("有人关注了你")
                            .targetType("user")
                            .targetId(targetUserId)
                            .createTime(System.currentTimeMillis())
                            .build()
                    );
                } catch (Exception e2) {
                    log.warn("MQ 发布关注通知失败: userId={}, targetUserId={}", userId, targetUserId, e2);
                }
            } catch (DuplicateKeyException e) {
                // 并发冲突：另一请求已插入，重查真实状态
                UserFollowPO conflict = queryFollow(userId, targetUserId);
                if (conflict.getStatus() == 1) {
                    return; // 已被置为已关注
                }
                boolean updated = StatusUpdateHelper.updateStatus(followMapper, conflict.getId(), 0, 1);
                if (updated) {
                    log.info("关注用户(并发恢复): userId={}, followUserId={}", userId, targetUserId);
                }
            }
            return;
        }

        if (exist.getStatus() == 0) {
            boolean updated = StatusUpdateHelper.updateStatus(followMapper, exist.getId(), 0, 1);
            if (updated) {
                log.info("恢复关注: userId={}, followUserId={}", userId, targetUserId);
            }
            return;
        }

        log.debug("关注用户(幂等忽略): userId={}, followUserId={}", userId, targetUserId);
    }

    /**
     * 取消关注目标用户（幂等，并发安全）
     * <p>目标：设为 status=0。仅在 status=1 时执行条件更新。
     *
     * @param userId       当前登录用户 ID
     * @param targetUserId 目标用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long userId, Long targetUserId) {
        UserFollowPO exist = queryFollow(userId, targetUserId);
        if (exist == null || exist.getStatus() == 0) {
            return;
        }

        boolean updated = StatusUpdateHelper.updateStatus(followMapper, exist.getId(), 1, 0);
        if (updated) {
            log.info("取消关注: userId={}, followUserId={}", userId, targetUserId);
        }
    }

    private UserFollowPO queryFollow(Long userId, Long targetUserId) {
        LambdaQueryWrapper<UserFollowPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollowPO::getUserId, userId)
                .eq(UserFollowPO::getFollowUserId, targetUserId);
        return followMapper.selectOne(wrapper);
    }

    /**
     * 查询当前用户是否关注了目标用户
     *
     * @param userId       当前登录用户 ID
     * @param targetUserId 目标用户 ID
     * @return true=已关注
     */
    @Override
    public boolean isFollowing(Long userId, Long targetUserId) {
        LambdaQueryWrapper<UserFollowPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollowPO::getUserId, userId)
                .eq(UserFollowPO::getFollowUserId, targetUserId)
                .eq(UserFollowPO::getStatus, CommonStatus.ACTIVE);
        return followMapper.selectCount(wrapper) > 0;
    }

    /**
     * 统计当前用户的关注数
     *
     * @param userId 用户 ID
     * @return 关注数
     */
    @Override
    public int countFollowing(Long userId) {
        return followMapper.countFollowing(userId);
    }

    /**
     * 统计当前用户的粉丝数
     *
     * @param userId 用户 ID
     * @return 粉丝数
     */
    @Override
    public int countFollowers(Long userId) {
        return followMapper.countFollowers(userId);
    }

    /**
     * 分页查询当前用户的关注列表
     * <p>结果中 following 字段始终为 true（因为都是自己关注的人）。
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 关注用户列表
     */
    @Override
    public PageResult<FollowUserVO> listFollowing(Long userId, int page, int size) {
        int total = followMapper.countFollowing(userId);
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        int offset = (page - 1) * size;
        List<FollowUserVO> records = followMapper.selectFollowingPage(userId, offset, size);
        // 关注列表里 following 始终为 true
        records.forEach(vo -> vo.setFollowing(true));
        return PageResult.of(records, total, page, size);
    }

    /**
     * 分页查询当前用户的粉丝列表
     * <p>批量查询当前用户已关注的用户 ID 集合，为每个粉丝补 following 字段（是否已回关）。
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 粉丝用户列表（含 following 回关状态）
     */
    @Override
    public PageResult<FollowUserVO> listFollowers(Long userId, int page, int size) {
        int total = followMapper.countFollowers(userId);
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        int offset = (page - 1) * size;
        List<FollowUserVO> records = followMapper.selectFollowersPage(userId, offset, size);

        // 查当前用户已关注的用户 ID 集合，批量补 following 字段
        List<Long> followingIds = followMapper.selectFollowingIds(userId);
        Set<Long> followingSet = followingIds.stream().collect(Collectors.toSet());
        records.forEach(vo -> vo.setFollowing(followingSet.contains(vo.getUserId())));

        return PageResult.of(records, total, page, size);
    }

    @Override
    public int countNewFollowers(Long userId) {
        return followMapper.countNewFollowers(userId);
    }

    @Override
    public List<Long> listFollowingUserIds(Long userId) {
        return followMapper.selectFollowingIds(userId);
    }
}