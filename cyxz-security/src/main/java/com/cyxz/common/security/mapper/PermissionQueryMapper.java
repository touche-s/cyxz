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
 * <p>仅提供权限校验所需的只读查询，不与 auth 模块自身的 SysUserRoleMapper 冲突
 * （auth 服务不会扫描本包）。
 */
@Mapper
public interface PermissionQueryMapper {

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
}
