package com.cyxz.circle.service;

import com.cyxz.common.base.PageResult;
import com.cyxz.circle.vo.CircleVO;
import com.cyxz.circle.vo.PublishableResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 圈子服务接口
 */
public interface CircleService {

    /**
     * 查询全量启用圈子列表
     */
    List<CircleVO> listAll(Long currentUserId);

    /**
     * 查询圈子详情
     */
    CircleVO getById(Long circleId, Long currentUserId);

    /**
     * 分页查询热门圈子（按成员数降序）
     */
    PageResult<CircleVO> listHot(int page, int size, Long currentUserId);

    /**
     * 加入圈子，重复加入幂等，事务内更新成员关系与 member_count
     */
    void joinCircle(Long userId, Long circleId);

    /**
     * 退出圈子，事务内更新成员关系与 member_count
     */
    void leaveCircle(Long userId, Long circleId);

    /**
     * 查询当前用户已加入的启用圈子
     */
    List<CircleVO> listJoined(Long userId);

    /**
     * 校验是否可在指定圈子发布（圈子存在、启用、已加入）
     */
    PublishableResult checkPublishable(Long circleId, Long userId);

    /**
     * 批量查询圈子名称
     */
    Map<Long, String> batchGetNames(Set<Long> circleIds);

    /**
     * 更新圈子资料
     */
    void updateCircle(Long circleId, String name, String intro, String avatar, String cover);

    /**
     * 创建圈子
     */
    CircleVO createCircle(String name, String intro, String avatar, String cover);

    /**
     * 删除圈子（软删除）
     */
    void deleteCircle(Long circleId);
}
