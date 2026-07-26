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

    /** 全量设置 post_count */
    @Update("UPDATE circle SET post_count = #{count} WHERE id = #{circleId}")
    void setPostCount(@Param("circleId") Long circleId, @Param("count") int count);

    /** 全量设置 member_count */
    @Update("UPDATE circle SET member_count = #{count} WHERE id = #{circleId}")
    void setMemberCount(@Param("circleId") Long circleId, @Param("count") int count);
}
