package com.cyxz.post.vo;

import lombok.Data;

/**
 * 帖子统计 VO
 * <p>用于数据中心展示用户已发布作品的聚合数据。
 */
@Data
public class PostStatsVO {

    /** 已发布作品数 */
    private long totalPosts;

    /** 总浏览量 */
    private long totalViews;

    /** 总点赞数 */
    private long totalLikes;

    /** 总收藏数 */
    private long totalCollections;
}
