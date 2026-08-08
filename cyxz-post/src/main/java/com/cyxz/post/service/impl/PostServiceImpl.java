package com.cyxz.post.service.impl;

import com.cyxz.common.base.PageResult;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.service.PostService;
import com.cyxz.post.vo.DashboardVO;
import com.cyxz.post.vo.PostInfoVO;
import com.cyxz.post.vo.PostStatsVO;
import com.cyxz.post.vo.PostVO;
import com.cyxz.post.vo.ReceivedLikeVO;
import com.cyxz.post.vo.TodayStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 帖子服务门面
 * <p>按职责拆分为 5 个子 Service，本类仅做委托转发，不包含业务逻辑：
 * <ul>
 *   <li>{@link PostCommandService} 写操作（创建/更新/删除/置顶/批量）</li>
 *   <li>{@link PostQueryService} 读操作（详情/列表/VO填充/缓存）</li>
 *   <li>{@link PostReviewService} 审核（人工/AI 结果处理/通知）</li>
 *   <li>{@link PostStatsService} 统计（仪表盘/排行/今日互动）</li>
 *   <li>{@link PostEsSyncService} ES 索引同步</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostCommandService postCommandService;
    private final PostQueryService postQueryService;
    private final PostReviewService postReviewService;
    private final PostStatsService postStatsService;

    @Override
    public Long createPost(Long userId, CreatePostRequest request) {
        return postCommandService.createPost(userId, request);
    }

    @Override
    public void updatePost(Long userId, UpdatePostRequest request) {
        postCommandService.updatePost(userId, request);
    }

    @Override
    public void deletePost(Long userId, Long postId) {
        postCommandService.deletePost(userId, postId);
    }

    @Override
    public void hardDeletePost(Long userId, Long postId) {
        postCommandService.hardDeletePost(userId, postId);
    }

    @Override
    public PostVO getById(Long postId, Long currentUserId) {
        return postQueryService.getById(postId, currentUserId);
    }

    @Override
    public PageResult<PostVO> listPosts(Long sectionId, Long circleId, String sortBy, int page, int size, Long currentUserId) {
        return postQueryService.listPosts(sectionId, circleId, sortBy, page, size, currentUserId);
    }

    @Override
    public PageResult<PostVO> listByUserId(Long userId, int page, int size, String sortField, String sortOrder) {
        return postQueryService.listByUserId(userId, page, size, sortField, sortOrder);
    }

    @Override
    public PageResult<PostVO> listByTargetUserId(Long targetUserId, Long currentUserId, int page, int size) {
        return postQueryService.listByTargetUserId(targetUserId, currentUserId, page, size);
    }

    @Override
    public PageResult<PostVO> listFavorites(Long targetUserId, Long currentUserId, int page, int size) {
        return postQueryService.listFavorites(targetUserId, currentUserId, page, size);
    }

    @Override
    public PageResult<PostVO> listFollowingPosts(Long userId, int page, int size) {
        return postQueryService.listFollowingPosts(userId, page, size);
    }

    @Override
    public PageResult<PostVO> listPendingReview(int page, int size) {
        return postQueryService.listPendingReview(page, size);
    }

    @Override
    public PageResult<PostVO> listPendingReviewByCircle(Long circleId, int page, int size) {
        return postQueryService.listPendingReviewByCircle(circleId, page, size);
    }

    @Override
    public PageResult<PostVO> listAllForAdmin(Integer status, String keyword, int page, int size) {
        return postQueryService.listAllForAdmin(status, keyword, page, size);
    }

    @Override
    public void adminDeletePost(Long postId) {
        postCommandService.adminDeletePost(postId);
    }

    @Override
    public void deletePostByCircle(Long circleId, Long postId) {
        postCommandService.deletePostByCircle(circleId, postId);
    }

    @Override
    public void pinPost(Long userId, Long postId) {
        postCommandService.pinPost(userId, postId);
    }

    @Override
    public void unpinPost(Long userId, Long postId) {
        postCommandService.unpinPost(userId, postId);
    }

    @Override
    public void batchOperate(Long userId, List<Long> postIds, String action) {
        postCommandService.batchOperate(userId, postIds, action);
    }

    @Override
    public void approvePost(Long postId) {
        postReviewService.approvePost(postId);
    }

    @Override
    public void rejectPost(Long postId, String reason) {
        postReviewService.rejectPost(postId, reason);
    }

    @Override
    public PostStatsVO getPostStats(Long userId) {
        return postStatsService.getPostStats(userId);
    }

    @Override
    public List<PostVO> getTopPosts(Long userId, int limit) {
        return postStatsService.getTopPosts(userId, limit);
    }

    @Override
    public TodayStatsVO getTodayStats(Long userId) {
        return postStatsService.getTodayStats(userId);
    }

    @Override
    public DashboardVO getDashboard(Long userId) {
        return postStatsService.getDashboard(userId);
    }

    @Override
    public Map<String, Object> getPostInfo(Long postId) {
        return postStatsService.getPostInfo(postId);
    }

    @Override
    public List<PostInfoVO> batchGetPostInfo(Set<Long> postIds) {
        return postStatsService.batchGetPostInfo(postIds);
    }

    @Override
    public PageResult<ReceivedLikeVO> getReceivedLikes(Long userId, int page, int size) {
        return postStatsService.getReceivedLikes(userId, page, size);
    }

    @Override
    public Map<Long, Integer> batchCountByCircle(Set<Long> circleIds) {
        return postStatsService.batchCountByCircle(circleIds);
    }
}
