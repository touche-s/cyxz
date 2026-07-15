package com.cyxz.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.vo.PostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

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

    /**
     * 原子更新浏览数
     *
     * @param postId 帖子 ID
     * @param delta  增量
     */
    @Update("UPDATE post SET views = views + #{delta} WHERE id = #{postId}")
    void updateViews(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * SQL 聚合统计用户帖子数据
     * <p>一条 SQL 查询当前用户所有已发布帖子的总数、总浏览、总点赞、总收藏。
     *
     * @param userId 用户 ID
     * @return 统计结果 Map（totalPosts, totalViews, totalLikes, totalCollections）
     */
    @Select("SELECT COUNT(*) AS totalPosts, " +
            "COALESCE(SUM(views), 0) AS totalViews, " +
            "COALESCE(SUM(likes), 0) AS totalLikes, " +
            "COALESCE(SUM(collections), 0) AS totalCollections " +
            "FROM post WHERE user_id = #{userId} AND status = 1")
    Map<String, Object> selectStatsByUserId(@Param("userId") Long userId);

    /**
     * 按浏览量倒序查询用户已发布帖子（排行榜）
     * <p>用于数据中心展示用户浏览量最高的 N 个帖子。
     *
     * @param userId 用户 ID
     * @param limit  返回条数
     * @return 帖子 PO 列表
     */
    @Select("SELECT * FROM post WHERE user_id = #{userId} AND status = 1 " +
            "ORDER BY views DESC LIMIT #{limit}")
    List<PostPO> selectTopPostsByViews(@Param("userId") Long userId, @Param("limit") int limit);
}
