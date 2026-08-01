package com.cyxz.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.comment.entity.CommentPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 评论 Mapper
 */
public interface CommentMapper extends BaseMapper<CommentPO> {

    /**
     * 原子更新点赞数
     *
     * @param commentId 评论 ID
     * @param delta     增量（+1 或 -1）
     */
    @Update("UPDATE comment SET likes = GREATEST(likes + #{delta}, 0) WHERE id = #{commentId}")
    void updateLikes(@Param("commentId") Long commentId, @Param("delta") int delta);

    /**
     * 统计今日某用户帖子收到的新评论数
     *
     * @param postAuthorId 帖子作者 ID
     * @return 今日新增评论数
     */
    @Select("SELECT COUNT(*) FROM comment WHERE post_author_id = #{postAuthorId} AND status = 1 AND create_time >= CURDATE()")
    int countTodayComments(@Param("postAuthorId") Long postAuthorId);
}
