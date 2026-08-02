package com.cyxz.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.user.entity.UserFollowPO;
import com.cyxz.user.vo.FollowUserVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserFollowMapper extends BaseMapper<UserFollowPO> {

    /**
     * UPSERT 关注记录：不存在则插入，存在则恢复为 status=1
     *
     * @return 1=新增, 2=恢复(0→1), 0=幂等(已是1)
     */
    @Insert("INSERT INTO user_follow(user_id, follow_user_id, status) " +
            "VALUES(#{userId}, #{targetUserId}, 1) " +
            "ON DUPLICATE KEY UPDATE status = 1")
    int upsertFollow(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    /**
     * 条件取消关注：仅 status=1 时更新为 0
     *
     * @return 1=取消成功, 0=无需取消(不存在或已取消)
     */
    @Update("UPDATE user_follow SET status = 0 WHERE user_id = #{userId} AND follow_user_id = #{targetUserId} AND status = 1")
    int deactivateFollow(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    @Select("SELECT COUNT(*) FROM user_follow WHERE user_id = #{userId} AND status = 1")
    int countFollowing(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_follow WHERE follow_user_id = #{userId} AND status = 1")
    int countFollowers(@Param("userId") Long userId);

    /**
     * SQL 分页查询关注列表（join user_profile 一次查出资料）
     */
    @Select("SELECT up.user_id AS userId, up.nickname AS nickname, up.avatar AS avatar, up.bio AS bio, uf.create_time AS createTime " +
            "FROM user_follow uf " +
            "INNER JOIN user_profile up ON uf.follow_user_id = up.user_id " +
            "WHERE uf.user_id = #{userId} AND uf.status = 1 " +
            "ORDER BY uf.create_time DESC " +
            "LIMIT #{offset}, #{size}")
    List<FollowUserVO> selectFollowingPage(@Param("userId") Long userId,
                                           @Param("offset") int offset,
                                           @Param("size") int size);

    /**
     * SQL 分页查询粉丝列表（join user_profile 一次查出资料）
     */
    @Select("SELECT up.user_id AS userId, up.nickname AS nickname, up.avatar AS avatar, up.bio AS bio, uf.create_time AS createTime " +
            "FROM user_follow uf " +
            "INNER JOIN user_profile up ON uf.user_id = up.user_id " +
            "WHERE uf.follow_user_id = #{userId} AND uf.status = 1 " +
            "ORDER BY uf.create_time DESC " +
            "LIMIT #{offset}, #{size}")
    List<FollowUserVO> selectFollowersPage(@Param("userId") Long userId,
                                           @Param("offset") int offset,
                                           @Param("size") int size);

    /**
     * 查询当前用户已关注的用户 ID 集合（用于粉丝列表补"是否已回关"字段）
     */
    @Select("SELECT follow_user_id FROM user_follow WHERE user_id = #{userId} AND status = 1")
    List<Long> selectFollowingIds(@Param("userId") Long userId);

    /**
     * 统计今日新增粉丝数（关注时间在今天及之后）
     *
     * @param userId 用户 ID
     * @return 今日新增粉丝数
     */
    @Select("SELECT COUNT(*) FROM user_follow WHERE follow_user_id = #{userId} AND status = 1 AND create_time >= CURDATE()")
    int countNewFollowers(@Param("userId") Long userId);
}
