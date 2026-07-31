package com.cyxz.circle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.circle.entity.CircleMemberPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

/**
 * 圈子成员 Mapper
 */
@Mapper
public interface CircleMemberMapper extends BaseMapper<CircleMemberPO> {

    /**
     * 查询用户已加入的圈子 ID 集合
     */
    @Select("SELECT circle_id FROM circle_member WHERE user_id = #{userId} AND status = 1")
    Set<Long> selectJoinedCircleIds(@Param("userId") Long userId);
}
