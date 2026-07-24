package com.cyxz.post.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.post.vo.CircleVO;

import java.util.List;

/**
 * 圈子服务接口
 */
public interface CircleService {

    List<CircleVO> listAll(Long currentUserId);

    CircleVO getById(Long circleId, Long currentUserId);

    PageResult<CircleVO> listHot(int page, int size, Long currentUserId);

    void joinCircle(Long userId, Long circleId);

    void leaveCircle(Long userId, Long circleId);

    List<CircleVO> listJoined(Long userId);
}
