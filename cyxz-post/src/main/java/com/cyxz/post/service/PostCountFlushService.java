package com.cyxz.post.service;

/**
 * 帖子计数刷库服务
 * <p>将 Redis Hash 中各维度增量刷入 MySQL post 表，由定时任务调度调用。
 */
public interface PostCountFlushService {

    /**
     * 刷浏览增量到 post.views
     *
     * @return 成功刷入条数
     */
    int flushViewCounts();

    /**
     * 刷点赞增量到 post.likes
     *
     * @return 成功刷入条数
     */
    int flushLikeCounts();

    /**
     * 刷收藏增量到 post.collections
     *
     * @return 成功刷入条数
     */
    int flushCollectCounts();

    /**
     * 刷评论数增量到 post.comments
     *
     * @return 成功刷入条数
     */
    int flushCommentCounts();
}
