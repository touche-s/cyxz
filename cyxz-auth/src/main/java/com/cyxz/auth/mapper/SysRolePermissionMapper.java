package com.cyxz.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.auth.entity.SysRolePermissionPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联 Mapper
 */
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermissionPO> {

    /**
     * 查询角色已分配的权限 ID 列表
     */
    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
