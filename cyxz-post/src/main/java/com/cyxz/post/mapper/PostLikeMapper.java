package com.cyxz.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.post.entity.PostLikePO;
import com.cyxz.post.vo.ReceivedLikeVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 帖子点赞关系 Mapper
 */
public interface PostLikeMapper extends BaseMapper<PostLikePO> {

    /**
     * UPSERT 帖子点赞：不存在则插入，存在则恢复为 status=1
     *
     * @return 1=新增, 2=恢复(0→1), 0=幂等(已是1)
     */
    @Insert("INSERT INTO post_like(post_id, user_id, status) " +
            "VALUES(#{postId}, #{userId}, 1) " +
            "ON DUPLICATE KEY UPDATE status = 1")
    int upsertLike(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 条件取消点赞：仅 status=1 时更新为 0
     *
     * @return 1=取消成功, 0=无需取消(不存在或已取消)
     */
    @Update("UPDATE post_like SET status = 0 WHERE post_id = #{postId} AND user_id = #{userId} AND status = 1")
    int deactivateLike(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 查询用户收到的点赞（JOIN post 表过滤当前用户的帖子）
     * <p>通过 INNER JOIN post 表确保只查询当前用户已发布帖子的点赞记录，
     * 按点赞时间倒序排列，支持分页。
     *
     * @param userId 用户 ID（帖子作者）
     * @param offset 分页偏移量
     * @param size   每页条数
     * @return 收到的点赞 VO 列表
     */
    @Select("SELECT pl.id AS likeId, pl.post_id AS postId, p.title AS postTitle, " +
            "pl.user_id AS userId, pl.create_time AS createTime " +
            "FROM post_like pl " +
            "INNER JOIN post p ON pl.post_id = p.id " +
            "WHERE p.user_id = #{userId} AND pl.status = 1 AND p.status = 2 " +
            "ORDER BY pl.create_time DESC " +
            "LIMIT #{offset}, #{size}")
    List<ReceivedLikeVO> selectReceivedLikes(@Param("userId") Long userId,
                                              @Param("offset") int offset,
                                              @Param("size") int size);

    /**
     * 统计用户收到的点赞总数
     * <p>统计当前用户所有已发布帖子收到的有效点赞数（status=1）。
     *
     * @param userId 用户 ID（帖子作者）
     * @return 点赞总数
     */
    @Select("SELECT COUNT(*) FROM post_like pl " +
            "INNER JOIN post p ON pl.post_id = p.id " +
            "WHERE p.user_id = #{userId} AND pl.status = 1 AND p.status = 2")
    int countReceivedLikes(@Param("userId") Long userId);
}
