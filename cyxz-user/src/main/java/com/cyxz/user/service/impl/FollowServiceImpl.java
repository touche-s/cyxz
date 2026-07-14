package com.cyxz.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.user.entity.UserFollowPO;
import com.cyxz.user.mapper.UserFollowMapper;
import com.cyxz.user.service.FollowService;
import com.cyxz.user.vo.FollowUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowMapper followMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能关注自己");
        }

        LambdaQueryWrapper<UserFollowPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollowPO::getUserId, userId)
                .eq(UserFollowPO::getFollowUserId, targetUserId);
        UserFollowPO exist = followMapper.selectOne(wrapper);

        if (exist == null) {
            UserFollowPO newFollow = new UserFollowPO();
            newFollow.setUserId(userId);
            newFollow.setFollowUserId(targetUserId);
            newFollow.setStatus(1);
            followMapper.insert(newFollow);
            log.info("关注用户: userId={}, followUserId={}", userId, targetUserId);
        } else if (exist.getStatus() == 0) {
            exist.setStatus(1);
            followMapper.updateById(exist);
            log.info("恢复关注: userId={}, followUserId={}", userId, targetUserId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long userId, Long targetUserId) {
        LambdaQueryWrapper<UserFollowPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollowPO::getUserId, userId)
                .eq(UserFollowPO::getFollowUserId, targetUserId);
        UserFollowPO exist = followMapper.selectOne(wrapper);

        if (exist != null && exist.getStatus() == 1) {
            exist.setStatus(0);
            followMapper.updateById(exist);
            log.info("取消关注: userId={}, followUserId={}", userId, targetUserId);
        }
    }

    @Override
    public boolean isFollowing(Long userId, Long targetUserId) {
        LambdaQueryWrapper<UserFollowPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollowPO::getUserId, userId)
                .eq(UserFollowPO::getFollowUserId, targetUserId)
                .eq(UserFollowPO::getStatus, 1);
        return followMapper.selectCount(wrapper) > 0;
    }

    @Override
    public int countFollowing(Long userId) {
        return followMapper.countFollowing(userId);
    }

    @Override
    public int countFollowers(Long userId) {
        return followMapper.countFollowers(userId);
    }

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
}
