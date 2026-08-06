package com.cyxz.circle.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 圈子角色分配 Mapper（跨库写）
 * <p>对 {@code cyxz_auth.sys_user_role} 表做圈子内角色的分配/撤销。
 * 圈子服务连的是 cyxz_circle 库，通过跨库表名前缀写入 auth 库的 RBAC 关联表，
 * DB 用户需具备 cyxz_auth 的写权限。
 * <p>INSERT IGNORE 保证幂等（主键为 user_id+role_id+circle_id，重复分配不报错）。
 */
@Mapper
public interface CircleRoleAssignmentMapper {

    /**
     * 分配圈子内角色（幂等）
     *
     * @param userId   用户 ID
     * @param roleId   角色 ID
     * @param circleId 圈子 ID
     * @return 实际插入行数（0 表示已存在）
     */
    @Insert("INSERT IGNORE INTO cyxz_auth.sys_user_role(user_id, role_id, circle_id) " +
            "VALUES(#{userId}, #{roleId}, #{circleId})")
    int assignRole(@Param("userId") Long userId,
                   @Param("roleId") Long roleId,
                   @Param("circleId") Long circleId);

    /**
     * 撤销圈子内角色
     *
     * @param userId   用户 ID
     * @param roleId   角色 ID
     * @param circleId 圈子 ID
     * @return 实际删除行数
     */
    @Delete("DELETE FROM cyxz_auth.sys_user_role " +
            "WHERE user_id = #{userId} AND role_id = #{roleId} AND circle_id = #{circleId}")
    int removeRole(@Param("userId") Long userId,
                   @Param("roleId") Long roleId,
                   @Param("circleId") Long circleId);
}
