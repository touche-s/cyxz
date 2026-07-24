package com.cyxz.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.post.entity.CirclePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 圈子 Mapper
 */
@Mapper
public interface CircleMapper extends BaseMapper<CirclePO> {

    @Update("UPDATE circle SET post_count = post_count + #{delta} WHERE id = #{circleId}")
    void updatePostCount(@Param("circleId") Long circleId, @Param("delta") int delta);

    @Update("UPDATE circle SET member_count = member_count + #{delta} WHERE id = #{circleId}")
    void updateMemberCount(@Param("circleId") Long circleId, @Param("delta") int delta);
}
