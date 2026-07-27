package com.cyxz.circle.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.circle.vo.CircleVO;
import com.cyxz.circle.vo.PublishableResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CircleService {

    List<CircleVO> listAll(Long currentUserId);

    CircleVO getById(Long circleId, Long currentUserId);

    PageResult<CircleVO> listHot(int page, int size, Long currentUserId);

    void joinCircle(Long userId, Long circleId);

    void leaveCircle(Long userId, Long circleId);

    List<CircleVO> listJoined(Long userId);

    PublishableResult checkPublishable(Long circleId, Long userId);

    Map<Long, String> batchGetNames(Set<Long> circleIds);
}
