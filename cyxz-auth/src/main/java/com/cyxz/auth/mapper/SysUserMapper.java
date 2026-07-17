package com.cyxz.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.auth.entity.SysUserPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserPO> {
}
