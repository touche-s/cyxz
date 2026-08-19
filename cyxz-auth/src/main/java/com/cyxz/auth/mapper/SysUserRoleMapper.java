package com.cyxz.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.auth.dto.UserRoleCode;
import com.cyxz.auth.entity.SysUserRolePO;
import com.cyxz.auth.vo.CircleMemberRoleVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * 用户-角色关联 Mapper
 * <p>对应 sys_user_role 表，支持按 user_id 查询全局角色（circle_id=0）与圈子内角色。
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRolePO> {

    /**
     * 查询用户的全局角色 code 列表（circle_id=0）
     *
     * @param userId 用户 ID
     * @return 角色 code 列表（如 SITE_OWNER / PLATFORM_ADMIN / USER）
     */
    @Select("SELECT r.code FROM sys_user_role ur " +
            "JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND ur.circle_id = 0")
    List<String> selectGlobalRoleCodes(@Param("userId") Long userId);

    /**
     * 查询所有用户的全局角色映射（用于管理后台用户列表）
     *
     * @return 用户 ID 到角色 code 的映射列表（每行：userId, roleCode）
     */
    @Select("SELECT ur.user_id AS userId, r.code AS roleCode FROM sys_user_role ur " +
            "JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.circle_id = 0")
    List<UserRoleCode> selectAllUserGlobalRoles();

    /**
     * 查询用户的全局权限码列表（基于 circle_id=0 的全局角色关联的权限）
     * <p>登录时调用，权限码写入 JWT 的 perms claim，供网关透传与前端 UI 显隐。
     *
     * @param userId 用户 ID
     * @return 全局权限码列表
     */
    @Select("SELECT p.code FROM sys_user_role ur " +
            "JOIN sys_role_permission rp ON rp.role_id = ur.role_id " +
            "JOIN sys_permission p ON p.id = rp.permission_id " +
            "WHERE ur.user_id = #{userId} AND ur.circle_id = 0")
    List<String> selectGlobalPermissionCodes(@Param("userId") Long userId);

    /**
     * 查询用户在指定圈子内的角色 ID 列表
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId} AND circle_id = #{circleId}")
    List<Long> selectCircleRoleIds(@Param("userId") Long userId, @Param("circleId") Long circleId);

    /**
     * 根据角色 ID 集合查询其拥有的权限码集合（去重）
     */
    @Select("<script>" +
            "SELECT DISTINCT p.code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON rp.permission_id = p.id " +
            "WHERE rp.role_id IN " +
            "<foreach item='id' collection='roleIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    Set<String> selectPermissionCodes(@Param("roleIds") List<Long> roleIds);

    /**
     * 查询用户管理的圈子 ID 列表（圈主或圈子管理员）
     */
    @Select("SELECT circle_id FROM sys_user_role " +
            "WHERE user_id = #{userId} AND circle_id > 0 " +
            "AND role_id IN (#{ownerRoleId}, #{adminRoleId})")
    List<Long> selectManagedCircleIds(@Param("userId") Long userId,
                                      @Param("ownerRoleId") long ownerRoleId,
                                      @Param("adminRoleId") long adminRoleId);

    /**
     * 查询圈子成员的角色信息（按圈主→管理员→成员排序）
     */
    @Select("SELECT u.id AS userId, u.username, ur.role_id AS roleId, r.code AS roleCode, r.label AS roleLabel, " +
            "DATE_FORMAT(ur.create_time, '%Y-%m-%d %H:%i') AS joinTime " +
            "FROM sys_user_role ur " +
            "JOIN sys_user u ON ur.user_id = u.id " +
            "JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.circle_id = #{circleId} " +
            "AND ur.role_id IN (#{ownerRoleId}, #{adminRoleId}, #{memberRoleId}) " +
            "ORDER BY FIELD(ur.role_id, #{ownerRoleId}, #{adminRoleId}, #{memberRoleId}), ur.create_time ASC")
    List<CircleMemberRoleVO> selectCircleMembers(@Param("circleId") Long circleId,
                                                 @Param("ownerRoleId") long ownerRoleId,
                                                 @Param("adminRoleId") long adminRoleId,
                                                 @Param("memberRoleId") long memberRoleId);

    /**
     * 分配圈子角色（INSERT IGNORE 幂等，重复分配不报错）
     */
    @Insert("INSERT IGNORE INTO sys_user_role(user_id, role_id, circle_id) " +
            "VALUES(#{userId}, #{roleId}, #{circleId})")
    int assignRole(@Param("userId") Long userId,
                   @Param("roleId") long roleId,
                   @Param("circleId") Long circleId);

    /**
     * 撤销圈子角色（幂等）
     */
    @Delete("DELETE FROM sys_user_role " +
            "WHERE user_id = #{userId} AND role_id = #{roleId} AND circle_id = #{circleId}")
    int removeRole(@Param("userId") Long userId,
                   @Param("roleId") long roleId,
                   @Param("circleId") Long circleId);
}
