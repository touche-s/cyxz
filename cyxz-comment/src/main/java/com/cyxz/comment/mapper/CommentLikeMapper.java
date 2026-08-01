package com.cyxz.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.comment.entity.CommentLikePO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 评论点赞关系 Mapper
 */
public interface CommentLikeMapper extends BaseMapper<CommentLikePO> {

    /**
     * UPSERT 评论点赞：不存在则插入，存在则恢复为 status=1
     *
     * @return 1=新增, 2=恢复(0→1), 0=幂等(已是1)
     */
    @Insert("INSERT INTO comment_like(comment_id, user_id, status) " +
            "VALUES(#{commentId}, #{userId}, 1) " +
            "ON DUPLICATE KEY UPDATE status = 1")
    int upsertLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 条件取消点赞：仅 status=1 时更新为 0
     *
     * @return 1=取消成功, 0=无需取消(不存在或已取消)
     */
    @Update("UPDATE comment_like SET status = 0 WHERE comment_id = #{commentId} AND user_id = #{userId} AND status = 1")
    int deactivateLike(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
