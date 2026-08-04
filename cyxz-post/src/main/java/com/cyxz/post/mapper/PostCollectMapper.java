package com.cyxz.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.post.entity.PostCollectPO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 帖子收藏关系 Mapper，对应 post_collect 表，用于帖子收藏关系的 UPSERT 及取消操作
 */
public interface PostCollectMapper extends BaseMapper<PostCollectPO> {

    /**
     * UPSERT 帖子收藏：不存在则插入，存在则恢复为 status=1
     * @param postId 帖子 ID
     * @param userId 收藏用户 ID
     * @return 1=新增, 2=恢复(0→1), 0=幂等(已是1)
     */
    @Insert("INSERT INTO post_collect(post_id, user_id, status) " +
            "VALUES(#{postId}, #{userId}, 1) " +
            "ON DUPLICATE KEY UPDATE status = 1")
    int upsertCollect(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 条件取消收藏：仅 status=1 时更新为 0
     * @param postId 帖子 ID
     * @param userId 收藏用户 ID
     * @return 1=取消成功, 0=无需取消(不存在或已取消)
     */
    @Update("UPDATE post_collect SET status = 0 WHERE post_id = #{postId} AND user_id = #{userId} AND status = 1")
    int deactivateCollect(@Param("postId") Long postId, @Param("userId") Long userId);
}
