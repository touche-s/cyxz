package com.cyxz.post.service.impl;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.utils.FeignResults;
import com.cyxz.comment.feign.CommentFeignClient;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.utils.UserFeignHelper;
import com.cyxz.user.vo.UserProfileVO;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.PostLikeMapper;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.api.vo.PostInfoVO;
import com.cyxz.post.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 帖子统计服务
 * <p>负责数据中心仪表盘、作品排行、今日互动、收到的点赞、批量帖子信息等统计查询。
 * 复用 QueryService 的 VO 填充能力。
 * <p>仪表盘 4 个独立查询用 {@code postQueryExecutor} 并行执行，避免串行累加延迟。
 */
@Slf4j
@Service
public class PostStatsService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final UserFeignClient userFeignClient;
    private final CommentFeignClient commentFeignClient;
    private final PostQueryService postQueryService;
    private final ExecutorService postQueryExecutor;

    public PostStatsService(PostMapper postMapper,
                            PostLikeMapper postLikeMapper,
                            UserFeignClient userFeignClient,
                            CommentFeignClient commentFeignClient,
                            PostQueryService postQueryService,
                            @Qualifier("postQueryExecutor") ExecutorService postQueryExecutor) {
        this.postMapper = postMapper;
        this.postLikeMapper = postLikeMapper;
        this.userFeignClient = userFeignClient;
        this.commentFeignClient = commentFeignClient;
        this.postQueryService = postQueryService;
        this.postQueryExecutor = postQueryExecutor;
    }

    /**
     * SQL 聚合统计用户帖子数据
     */
    public PostStatsVO getPostStats(Long userId) {
        PostStatsVO stats = postMapper.selectStatsByUserId(userId);
        if (stats == null) {
            stats = new PostStatsVO();
        }
        return stats;
    }

    /**
     * 查询用户作品排行榜（按浏览量倒序）
     */
    public List<PostVO> getTopPosts(Long userId, int limit) {
        List<PostPO> topPosts = postMapper.selectTopPostsByViews(userId, limit);
        if (topPosts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, topPosts.stream().map(PostPO::getUserId).collect(Collectors.toSet()));
        Map<Long, String> circleNameMap = postQueryService.extractCircleNameMap(topPosts);
        Map<Long, String> sectionNameMap = postQueryService.extractSectionNameMap(topPosts);
        return topPosts.stream()
                .map(po -> postQueryService.convertToVO(po, userMap, circleNameMap, sectionNameMap, Collections.emptySet(), Collections.emptySet()))
                .collect(Collectors.toList());
    }

    /**
     * 获取今日新增互动统计
     */
    public TodayStatsVO getTodayStats(Long userId) {
        TodayStatsVO stats = postMapper.selectTodayStats(userId);
        if (stats == null) {
            stats = new TodayStatsVO(0, 0, 0);
        }
        try {
            Integer todayComments = FeignResults.unwrapOrNull(commentFeignClient.countTodayComments(userId));
            if (todayComments != null) {
                stats.setTodayComments(todayComments);
            }
        } catch (Exception e) {
            log.warn("获取今日评论数失败: userId={}", userId, e);
        }
        return stats;
    }

    /**
     * 获取数据中心仪表盘数据
     * <p>4 个独立查询并行执行（总览统计 / 月度趋势 / 日度趋势 / Top 作品），
     * 总延迟从 4 次串行 DB 往返降为 1 次往返（取最慢者）。
     */
    public DashboardVO getDashboard(Long userId) {
        CompletableFuture<PostStatsVO> summaryFuture =
                CompletableFuture.supplyAsync(() -> {
                    PostStatsVO s = postMapper.selectStatsByUserId(userId);
                    return s != null ? s : new PostStatsVO();
                }, postQueryExecutor);
        CompletableFuture<List<DashboardVO.MonthlyTrendVO>> trendsFuture =
                CompletableFuture.supplyAsync(() -> {
                    List<DashboardVO.MonthlyTrendVO> t = postMapper.selectMonthlyTrends(userId);
                    return t != null ? t : Collections.<DashboardVO.MonthlyTrendVO>emptyList();
                }, postQueryExecutor);
        CompletableFuture<List<DashboardVO.DailyTrendVO>> dailyFuture =
                CompletableFuture.supplyAsync(() -> {
                    List<DashboardVO.DailyTrendVO> d = postMapper.selectDailyTrends(userId);
                    return d != null ? d : Collections.<DashboardVO.DailyTrendVO>emptyList();
                }, postQueryExecutor);
        CompletableFuture<List<PostVO>> topFuture =
                CompletableFuture.supplyAsync(() -> getTopPosts(userId, 5), postQueryExecutor);

        CompletableFuture.allOf(summaryFuture, trendsFuture, dailyFuture, topFuture).join();

        DashboardVO dashboard = new DashboardVO();
        dashboard.setSummary(summaryFuture.join());
        dashboard.setMonthlyTrends(trendsFuture.join());
        dashboard.setDailyTrends(dailyFuture.join());
        dashboard.setSectionDistribution(Collections.emptyList());
        dashboard.setTopPosts(topFuture.join());
        return dashboard;
    }

    /**
     * 获取帖子信息（内部接口）
     */
    public PostInfoVO getPostInfo(Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            return null;
        }
        PostInfoVO vo = new PostInfoVO();
        vo.setPostId(po.getId());
        vo.setUserId(po.getUserId());
        vo.setTitle(po.getTitle());
        vo.setCircleId(po.getCircleId());
        return vo;
    }

    /**
     * 批量获取帖子简要信息（内部接口）
     */
    public List<PostInfoVO> batchGetPostInfo(Set<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<PostPO> posts = postMapper.selectBatchIds(postIds);
        return posts.stream().map(po -> {
            PostInfoVO vo = new PostInfoVO();
            vo.setPostId(po.getId());
            vo.setUserId(po.getUserId());
            vo.setTitle(po.getTitle());
            vo.setCircleId(po.getCircleId());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 查询用户收到的点赞列表
     */
    public PageResult<ReceivedLikeVO> getReceivedLikes(Long userId, int page, int size) {
        int total = postLikeMapper.countReceivedLikes(userId);
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        int offset = (page - 1) * size;
        List<ReceivedLikeVO> records = postLikeMapper.selectReceivedLikes(userId, offset, size);

        // 批量查用户信息
        Set<Long> userIds = records.stream()
                .map(ReceivedLikeVO::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserProfileVO> userMap = UserFeignHelper.batchGetUsers(userFeignClient, userIds);
        records.forEach(vo -> {
            UserProfileVO user = userMap.get(vo.getUserId());
            if (user != null) {
                vo.setUserName(user.getNickname());
                vo.setUserAvatar(user.getAvatar());
            }
        });

        return PageResult.of(records, total, page, size);
    }

    /**
     * 批量统计各圈子的已发布帖子数（无帖子的圈子返回 0）
     */
    public Map<Long, Integer> batchCountByCircle(Set<Long> circleIds) {
        if (circleIds == null || circleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = postMapper.batchCountByCircleIds(circleIds);
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long circleId = ((Number) row.get("circle_id")).longValue();
            Integer cnt = ((Number) row.get("cnt")).intValue();
            result.put(circleId, cnt);
        }
        // 没有帖子的圈子返回 0
        for (Long circleId : circleIds) {
            result.putIfAbsent(circleId, 0);
        }
        return result;
    }
}
