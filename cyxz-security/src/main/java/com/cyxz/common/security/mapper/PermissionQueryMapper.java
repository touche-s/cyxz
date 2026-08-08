package com.cyxz.common.security.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * 权限查询 Mapper（跨库只读）
 * <p>RBAC 表（sys_user_role / sys_role_permission / sys_permission）位于 {@code cyxz_auth} 库，
 * circle 表位于 {@code cyxz_circle} 库。各业务微服务连的是各自库（同 MySQL 实例、同 root 账号），
 * 这里用跨库表名前缀查询，DB 用户需具备两个 schema 的读权限。
 */
@Mapper
public interface PermissionQueryMapper {

    /**
     * 查询用户的全局角色 code 列表（circle_id=0）
     *
     * @param userId 用户 ID
     * @return 角色 code 列表（如 SITE_OWNER / PLATFORM_ADMIN / USER）
     */
    @Select("SELECT r.code FROM cyxz_auth.sys_user_role ur " +
            "JOIN cyxz_auth.sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND ur.circle_id = 0")
    List<String> selectGlobalRoleCodes(@Param("userId") Long userId);

    /**
     * 查询用户的全局权限码集合（基于 circle_id=0 的全局角色）
     *
     * @param userId 用户 ID
     * @return 全局权限码集合
     */
    @Select("SELECT DISTINCT p.code FROM cyxz_auth.sys_user_role ur " +
            "JOIN cyxz_auth.sys_role_permission rp ON rp.role_id = ur.role_id " +
            "JOIN cyxz_auth.sys_permission p ON p.id = rp.permission_id " +
            "WHERE ur.user_id = #{userId} AND ur.circle_id = 0")
    Set<String> selectGlobalPermissionCodes(@Param("userId") Long userId);

    /**
     * 查询用户在指定圈子内的角色 ID 列表
     *
     * @param userId   用户 ID
     * @param circleId 圈子 ID
     * @return 角色 ID 列表
     */
    @Select("SELECT role_id FROM cyxz_auth.sys_user_role " +
            "WHERE user_id = #{userId} AND circle_id = #{circleId}")
    List<Long> selectCircleRoleIds(@Param("userId") Long userId,
                                   @Param("circleId") Long circleId);

    /**
     * 根据角色 ID 集合查询其拥有的权限码集合
     *
     * @param roleIds 角色 ID 集合
     * @return 权限码集合
     */
    @Select("<script>" +
            "SELECT p.code FROM cyxz_auth.sys_permission p " +
            "INNER JOIN cyxz_auth.sys_role_permission rp ON rp.permission_id = p.id " +
            "WHERE rp.role_id IN " +
            "<foreach item='id' collection='roleIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    Set<String> selectPermissionCodes(@Param("roleIds") List<Long> roleIds);

    /**
     * 校验圈子 ID 是否真实存在
     *
     * @param circleId 圈子 ID
     * @return 存在记录数
     */
    @Select("SELECT COUNT(1) FROM cyxz_circle.circle WHERE id = #{circleId}")
    int countCircleById(@Param("circleId") Long circleId);

    /**
     * 查询拥有指定角色的用户 ID 及对应 circle_id（角色权限配置变更时失效缓存用）
     *
     * @param roleId 角色 ID
     * @return 用户-圈子关联记录列表
     */
    @Select("SELECT user_id, circle_id FROM cyxz_auth.sys_user_role WHERE role_id = #{roleId}")
    List<UserCircleEntry> selectUsersByRoleId(@Param("roleId") Long roleId);

    /**
     * 用户-圈子关联记录（用于缓存失效时遍历）
     */
    class UserCircleEntry {
        private Long userId;
        private Long circleId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getCircleId() { return circleId; }
        public void setCircleId(Long circleId) { this.circleId = circleId; }
    }
}
