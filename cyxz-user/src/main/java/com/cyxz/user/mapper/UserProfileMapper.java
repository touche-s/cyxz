package com.cyxz.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.user.entity.UserProfilePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户资料 Mapper
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfilePO> {
}
