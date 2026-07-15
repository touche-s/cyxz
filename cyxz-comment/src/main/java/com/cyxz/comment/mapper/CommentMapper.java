package com.cyxz.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.comment.entity.CommentPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 评论 Mapper
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentPO> {

    /**
     * 原子更新点赞数
     *
     * @param commentId 评论 ID
     * @param delta     增量（+1 或 -1）
     */
    @Update("UPDATE comment SET likes = GREATEST(likes + #{delta}, 0) WHERE id = #{commentId}")
    void updateLikes(@Param("commentId") Long commentId, @Param("delta") int delta);
}
