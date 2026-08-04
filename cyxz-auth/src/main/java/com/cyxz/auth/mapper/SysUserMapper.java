package com.cyxz.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.auth.entity.SysUserPO;

/**
 * 系统用户 Mapper
 * <p>对应 sys_user 表，负责账号认证相关字段（用户名、密码、状态、角色）的 CRUD，
 * 用户展示资料由 cyxz-user 服务管理；继承 MyBatis-Plus BaseMapper 获得通用增删改查能力。
 */
public interface SysUserMapper extends BaseMapper<SysUserPO> {
}
