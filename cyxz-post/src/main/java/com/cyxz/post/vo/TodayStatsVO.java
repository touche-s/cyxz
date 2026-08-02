package com.cyxz.post.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 今日统计 VO
 * <p>用于创作首页工作台展示今日新增互动数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodayStatsVO {

    /** 今日新增点赞数 */
    private int todayLikes;

    /** 今日新增收藏数 */
    private int todayCollections;

    /** 今日新增评论数 */
    private int todayComments;
}
