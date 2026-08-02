package com.cyxz.circle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.circle.entity.CirclePO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 圈子 Mapper，提供帖子数和成员数的原子更新
 */
public interface CircleMapper extends BaseMapper<CirclePO> {

    /**
     * 原子增减帖子数（delta 可正可负，不小于 0）
     */
    @Update("UPDATE circle SET post_count = GREATEST(post_count + #{delta}, 0) WHERE id = #{circleId}")
    void updatePostCount(@Param("circleId") Long circleId, @Param("delta") int delta);

    /**
     * 原子增减成员数（delta 可正可负，不小于 0）
     */
    @Update("UPDATE circle SET member_count = GREATEST(member_count + #{delta}, 0) WHERE id = #{circleId}")
    void updateMemberCount(@Param("circleId") Long circleId, @Param("delta") int delta);

    /**
     * 覆盖帖子数
     */
    @Update("UPDATE circle SET post_count = #{count} WHERE id = #{circleId}")
    void setPostCount(@Param("circleId") Long circleId, @Param("count") int count);

    /**
     * 覆盖成员数
     */
    @Update("UPDATE circle SET member_count = #{count} WHERE id = #{circleId}")
    void setMemberCount(@Param("circleId") Long circleId, @Param("count") int count);
}
