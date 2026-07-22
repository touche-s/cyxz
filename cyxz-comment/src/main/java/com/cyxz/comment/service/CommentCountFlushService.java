package com.cyxz.comment.service;

/**
 * 评论计数刷库服务
 * <p>将 Redis Hash 中评论点赞增量刷入 MySQL comment.likes，由定时任务调度调用。
 */
public interface CommentCountFlushService {

    /**
     * 刷评论点赞增量到 comment.likes
     *
     * @return 成功刷入条数
     */
    int flushLikeCounts();
}
