package com.cyxz.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.auth.dto.UserRoleCode;
import com.cyxz.auth.entity.SysUserRolePO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
}
