package com.cyxz.circle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.circle.entity.CircleMemberPO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Set;

/**
 * 圈子成员 Mapper
 */
public interface CircleMemberMapper extends BaseMapper<CircleMemberPO> {

    /**
     * UPSERT 圈子成员：不存在则插入，存在则恢复为 status=1
     *
     * @return 1=新增, 2=恢复(0→1), 0=幂等(已是1)
     */
    @Insert("INSERT INTO circle_member(circle_id, user_id, status) " +
            "VALUES(#{circleId}, #{userId}, 1) " +
            "ON DUPLICATE KEY UPDATE status = 1")
    int upsertMember(@Param("circleId") Long circleId, @Param("userId") Long userId);

    /**
     * 条件退出圈子：仅 status=1 时更新为 0
     *
     * @return 1=退出成功, 0=无需退出(不存在或已退出)
     */
    @Update("UPDATE circle_member SET status = 0 WHERE circle_id = #{circleId} AND user_id = #{userId} AND status = 1")
    int deactivateMember(@Param("circleId") Long circleId, @Param("userId") Long userId);

    /**
     * 查询用户已加入的圈子 ID 集合
     */
    @Select("SELECT circle_id FROM circle_member WHERE user_id = #{userId} AND status = 1")
    Set<Long> selectJoinedCircleIds(@Param("userId") Long userId);
}
