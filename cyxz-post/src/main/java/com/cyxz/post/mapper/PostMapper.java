package com.cyxz.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.vo.DashboardVO;
import com.cyxz.post.vo.PostStatsVO;
import com.cyxz.post.vo.PostVO;
import com.cyxz.post.vo.TodayStatsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 帖子 Mapper
 */
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
     * 原子更新评论数
     *
     * @param postId 帖子 ID
     * @param delta  增量（+1 或 -1）
     */
    @Update("UPDATE post SET comments = GREATEST(comments + #{delta}, 0) WHERE id = #{postId}")
    void updateComments(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * SQL 聚合统计用户帖子数据
     * <p>一条 SQL 查询当前用户所有已发布帖子的总数、总浏览、总点赞、总收藏。
     *
     * @param userId 用户 ID
     * @return 统计结果 VO，无数据返回 null
     */
    @Select("SELECT COUNT(*) AS totalPosts, " +
            "COALESCE(SUM(views), 0) AS totalViews, " +
            "COALESCE(SUM(likes), 0) AS totalLikes, " +
            "COALESCE(SUM(collections), 0) AS totalCollections " +
            "FROM post WHERE user_id = #{userId} AND status = 2")
    PostStatsVO selectStatsByUserId(@Param("userId") Long userId);

    /**
     * 按浏览量倒序查询用户已发布帖子（排行榜）
     * <p>用于数据中心展示用户浏览量最高的 N 个帖子。
     *
     * @param userId 用户 ID
     * @param limit  返回条数
     * @return 帖子 PO 列表
     */
    @Select("SELECT * FROM post WHERE user_id = #{userId} AND status = 2 " +
            "ORDER BY views DESC LIMIT #{limit}")
    List<PostPO> selectTopPostsByViews(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 按月聚合用户已发布作品数据
     * <p>按 create_time 的 %Y-%m 分组，统计每月新增作品数、总浏览、总点赞。
     *
     * @param userId 用户 ID
     * @return 月度趋势列表
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, " +
            "COUNT(*) AS posts, " +
            "COALESCE(SUM(views), 0) AS views, " +
            "COALESCE(SUM(likes), 0) AS likes " +
            "FROM post WHERE user_id = #{userId} AND status = 2 " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m') " +
            "ORDER BY month ASC")
    List<DashboardVO.MonthlyTrendVO> selectMonthlyTrends(@Param("userId") Long userId);

    /**
     * 按日聚合用户已发布作品数据（近 30 天）
     * <p>按 create_time 的 %m-%d 分组，统计每日新增作品数、总浏览、总点赞。
     *
     * @param userId 用户 ID
     * @return 每日趋势列表
     */
    @Select("SELECT DATE_FORMAT(create_time, '%m-%d') AS date, " +
            "COUNT(*) AS posts, " +
            "COALESCE(SUM(views), 0) AS views, " +
            "COALESCE(SUM(likes), 0) AS likes " +
            "FROM post WHERE user_id = #{userId} AND status = 2 " +
            "AND create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%m-%d') " +
            "ORDER BY date ASC")
    List<DashboardVO.DailyTrendVO> selectDailyTrends(@Param("userId") Long userId);

    /**
     * 查询今日新增互动统计
     * <p>一条 SQL 通过子查询统计今天该用户帖子收到的新点赞、新收藏数。
     *
     * @param userId 用户 ID
     * @return 今日统计 VO（todayComments 为 0，由 Service 层调用评论服务获取）
     */
    @Select("SELECT " +
            "COALESCE((SELECT COUNT(*) FROM post_like pl INNER JOIN post p ON pl.post_id = p.id " +
            "WHERE p.user_id = #{userId} AND pl.status = 1 AND pl.create_time >= CURDATE()), 0) AS todayLikes, " +
            "COALESCE((SELECT COUNT(*) FROM post_collect pc INNER JOIN post p ON pc.post_id = p.id " +
            "WHERE p.user_id = #{userId} AND pc.status = 1 AND pc.create_time >= CURDATE()), 0) AS todayCollections, " +
            "0 AS todayComments")
    TodayStatsVO selectTodayStats(@Param("userId") Long userId);

    /**
     * 原子置顶帖子（防 TOCTOU 竞态）
     * <p>单条 SQL 完成：校验帖子归属 + 未置顶 + 已置顶数 < 3，全部满足才更新。
     * <p>rows=0 表示帖子不存在/非本人/已置顶/已达上限。
     */
    @Update("UPDATE post SET is_pinned = 1, pinned_time = NOW() " +
            "WHERE id = #{postId} AND user_id = #{userId} AND is_pinned = 0 " +
            "AND (SELECT cnt FROM (SELECT COUNT(*) AS cnt FROM post WHERE user_id = #{userId} AND is_pinned = 1) t) < 3")
    int pinPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 取消置顶
     */
    @Update("UPDATE post SET is_pinned = 0, pinned_time = NULL WHERE id = #{postId} AND user_id = #{userId}")
    int unpinPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 统计当前用户已置顶帖子数
     */
    @Select("SELECT COUNT(*) FROM post WHERE user_id = #{userId} AND is_pinned = 1")
    int countPinnedPosts(@Param("userId") Long userId);

    /**
     * 批量更新帖子状态
     *
     * @param fromStatus 限制仅更新该状态的帖子，null 表示不限制（用于状态机校验）
     */
    @Update("<script>" +
            "UPDATE post SET status = #{status} WHERE user_id = #{userId} AND id IN " +
            "<foreach collection='postIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "<if test='fromStatus != null'> AND status = #{fromStatus}</if>" +
            "</script>")
    int batchUpdateStatus(@Param("userId") Long userId, @Param("postIds") List<Long> postIds,
                          @Param("status") int status, @Param("fromStatus") Integer fromStatus);

    /**
     * 统计各圈子的已发布帖子数
     */
    @Select("<script>" +
            "SELECT circle_id, COUNT(*) AS cnt FROM post WHERE circle_id IN " +
            "<foreach collection='circleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            " AND status = 2 GROUP BY circle_id" +
            "</script>")
    List<Map<String, Object>> batchCountByCircleIds(@Param("circleIds") Set<Long> circleIds);
}
