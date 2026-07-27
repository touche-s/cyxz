package com.cyxz.circle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.circle.entity.CircleMemberPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface CircleMemberMapper extends BaseMapper<CircleMemberPO> {

    @Select("SELECT circle_id FROM circle_member WHERE user_id = #{userId} AND status = 1")
    Set<Long> selectJoinedCircleIds(@Param("userId") Long userId);
}
