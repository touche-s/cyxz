package com.cyxz.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.post.entity.PostPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 帖子 Mapper
 */
@Mapper
public interface PostMapper extends BaseMapper<PostPO> {

    /**
     * 原子更新点赞数
     *
     * @param postId 帖子 ID
     * @param delta  增量（+1 或 -1）
     */
    @Update("UPDATE post SET likes = GREATEST(likes + #{delta}, 0) WHERE id = #{postId}")
    void updateLikes(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * 原子更新收藏数
     *
     * @param postId 帖子 ID
     * @param delta  增量（+1 或 -1）
     */
    @Update("UPDATE post SET collections = GREATEST(collections + #{delta}, 0) WHERE id = #{postId}")
    void updateCollections(@Param("postId") Long postId, @Param("delta") int delta);
}
