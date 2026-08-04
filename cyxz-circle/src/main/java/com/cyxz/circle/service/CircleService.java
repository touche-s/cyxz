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
     * @param currentUserId 当前登录用户 ID，用于回填 joined 字段，可为 null
     * @return 启用状态的圈子 VO 列表，按 sort_order 升序
     */
    List<CircleVO> listAll(Long currentUserId);

    /**
     * 查询圈子详情
     * @param circleId 圈子 ID
     * @param currentUserId 当前登录用户 ID，用于回填 joined 字段，可为 null
     * @return 圈子 VO，圈子不存在或未启用时抛出业务异常
     */
    CircleVO getById(Long circleId, Long currentUserId);

    /**
     * 分页查询热门圈子（按成员数降序）
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @param currentUserId 当前登录用户 ID，用于回填 joined 字段，可为 null
     * @return 启用圈子分页结果，按 member_count 降序
     */
    PageResult<CircleVO> listHot(int page, int size, Long currentUserId);

    /**
     * 加入圈子，重复加入幂等，事务内更新成员关系与 member_count
     * @param userId 用户 ID
     * @param circleId 圈子 ID
     */
    void joinCircle(Long userId, Long circleId);

    /**
     * 退出圈子，事务内更新成员关系与 member_count
     * @param userId 用户 ID
     * @param circleId 圈子 ID
     */
    void leaveCircle(Long userId, Long circleId);

    /**
     * 查询当前用户已加入的启用圈子
     * @param userId 用户 ID
     * @return 用户已加入且启用的圈子 VO 列表
     */
    List<CircleVO> listJoined(Long userId);

    /**
     * 校验是否可在指定圈子发布（圈子存在、启用、已加入）
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @return 发布校验结果，包含 exists、enabled、joined、publishable 字段
     */
    PublishableResult checkPublishable(Long circleId, Long userId);

    /**
     * 批量查询圈子名称
     * @param circleIds 圈子 ID 集合
     * @return circleId → 圈子名称 的映射，入参为空时返回空 Map
     */
    Map<Long, String> batchGetNames(Set<Long> circleIds);

    /**
     * 更新圈子资料
     * @param circleId 圈子 ID
     * @param name 圈子名称，非空白时更新
     * @param intro 圈子简介，非 null 时更新
     * @param avatar 圈子头像 URL，非空白时更新
     * @param cover 圈子封面 URL，非空白时更新
     */
    void updateCircle(Long circleId, String name, String intro, String avatar, String cover);

    /**
     * 创建圈子
     * @param name 圈子名称，不能为空
     * @param intro 圈子简介，可为 null
     * @param avatar 圈子头像 URL，可为 null
     * @param cover 圈子封面 URL，可为 null
     * @return 创建后的圈子 VO，并初始化默认板块
     */
    CircleVO createCircle(String name, String intro, String avatar, String cover);

    /**
     * 删除圈子（软删除）
     * @param circleId 圈子 ID
     */
    void deleteCircle(Long circleId);
}
