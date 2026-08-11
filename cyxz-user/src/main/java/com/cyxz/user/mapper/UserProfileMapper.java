package com.cyxz.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.user.entity.UserProfilePO;

/**
 * 用户资料 Mapper
 * <p>对应 user_profile 表，与 sys_user 一一对应，存储昵称、头像、性别、简介、生日等展示信息，
 * 继承 MyBatis-Plus BaseMapper 获得通用增删改查能力。
 */
public interface UserProfileMapper extends BaseMapper<UserProfilePO> {
}
