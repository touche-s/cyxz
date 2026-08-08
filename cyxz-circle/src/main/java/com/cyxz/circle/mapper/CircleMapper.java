package com.cyxz.circle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.circle.entity.CirclePO;
import com.cyxz.circle.vo.MemberVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 圈子 Mapper，提供帖子数和成员数的原子更新
 */
public interface CircleMapper extends BaseMapper<CirclePO> {

    /**
     * 原子增减帖子数（delta 可正可负，不小于 0）
     * @param circleId 圈子 ID
     * @param delta 增量（正数加，负数减，结果不低于 0）
     */
    @Update("UPDATE circle SET post_count = GREATEST(post_count + #{delta}, 0) WHERE id = #{circleId}")
    void updatePostCount(@Param("circleId") Long circleId, @Param("delta") int delta);

    /**
     * 原子增减成员数（delta 可正可负，不小于 0）
     * @param circleId 圈子 ID
     * @param delta 增量（正数加，负数减，结果不低于 0）
     */
    @Update("UPDATE circle SET member_count = GREATEST(member_count + #{delta}, 0) WHERE id = #{circleId}")
    void updateMemberCount(@Param("circleId") Long circleId, @Param("delta") int delta);

    /**
     * 覆盖帖子数
     * @param circleId 圈子 ID
     * @param count 待设置的帖子数
     */
    @Update("UPDATE circle SET post_count = #{count} WHERE id = #{circleId}")
    void setPostCount(@Param("circleId") Long circleId, @Param("count") int count);

    /**
     * 覆盖成员数
     * @param circleId 圈子 ID
     * @param count 待设置的成员数
     */
    @Update("UPDATE circle SET member_count = #{count} WHERE id = #{circleId}")
    void setMemberCount(@Param("circleId") Long circleId, @Param("count") int count);

    /**
     * 查询用户管理的圈子（圈主 CIRCLE_OWNER 或圈子管理员 CIRCLE_ADMIN）
     * <p>跨库关联 cyxz_auth.sys_user_role，role_id 由调用方传入 CircleRoleConstants 常量。
     * 用于前端圈子管理后台的左侧圈子选择器。
     *
     * @param userId       用户 ID
     * @param ownerRoleId  圈主角色 ID（CircleRoleConstants.CIRCLE_OWNER_ROLE_ID）
     * @param adminRoleId  圈子管理员角色 ID（CircleRoleConstants.CIRCLE_ADMIN_ROLE_ID）
     * @return 用户管理的启用圈子列表
     */
    @Select("SELECT c.* FROM circle c " +
            "INNER JOIN cyxz_auth.sys_user_role ur ON ur.circle_id = c.id " +
            "WHERE ur.user_id = #{userId} AND ur.role_id IN (#{ownerRoleId}, #{adminRoleId}) AND c.status = 1 " +
            "ORDER BY c.sort_order ASC")
    List<CirclePO> selectManagedCircles(@Param("userId") Long userId,
                                        @Param("ownerRoleId") long ownerRoleId,
                                        @Param("adminRoleId") long adminRoleId);

    /**
     * 查询圈子成员列表（含角色信息），按角色和加入时间排序
     * <p>跨库关联 cyxz_auth 的 sys_user_role、sys_user、sys_role 表。
     *
     * @param circleId         圈子 ID
     * @param ownerRoleId      圈主角色 ID
     * @param adminRoleId      圈子管理员角色 ID
     * @param memberRoleId     圈子成员角色 ID
     * @return 成员列表（含用户信息和角色）
     */
    @Select("SELECT u.id AS userId, u.username, u.nickname, u.avatar, " +
            "r.code AS roleCode, r.label AS roleLabel, " +
            "DATE_FORMAT(ur.create_time, '%Y-%m-%d %H:%i') AS joinTime " +
            "FROM cyxz_auth.sys_user_role ur " +
            "JOIN cyxz_auth.sys_user u ON ur.user_id = u.id " +
            "JOIN cyxz_auth.sys_role r ON ur.role_id = r.id " +
            "WHERE ur.circle_id = #{circleId} " +
            "AND ur.role_id IN (#{ownerRoleId}, #{adminRoleId}, #{memberRoleId}) " +
            "ORDER BY FIELD(ur.role_id, #{ownerRoleId}, #{adminRoleId}, #{memberRoleId}), ur.create_time ASC")
    List<MemberVO> selectMembersByCircleId(@Param("circleId") Long circleId,
                                           @Param("ownerRoleId") long ownerRoleId,
                                           @Param("adminRoleId") long adminRoleId,
                                           @Param("memberRoleId") long memberRoleId);

    /**
     * 查询用户在圈子中的角色 ID 列表
     *
     * @param userId   用户 ID
     * @param circleId 圈子 ID
     * @return 角色 ID 列表
     */
    @Select("SELECT role_id FROM cyxz_auth.sys_user_role WHERE user_id = #{userId} AND circle_id = #{circleId}")
    List<Long> selectUserRoleIdsInCircle(@Param("userId") Long userId, @Param("circleId") Long circleId);
}
