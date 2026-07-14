package com.cyxz.user.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.user.vo.FollowUserVO;

public interface FollowService {

    void follow(Long userId, Long targetUserId);

    void unfollow(Long userId, Long targetUserId);

    boolean isFollowing(Long userId, Long targetUserId);

    int countFollowing(Long userId);

    int countFollowers(Long userId);

    PageResult<FollowUserVO> listFollowing(Long userId, int page, int size);

    PageResult<FollowUserVO> listFollowers(Long userId, int page, int size);
}
