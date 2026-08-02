package com.cyxz.post.vo;

import lombok.Data;

import java.util.List;

/**
 * 数据中心仪表盘 VO
 * <p>包含概览统计、月度趋势、板块分布和 Top 作品。
 */
@Data
public class DashboardVO {

    /** 概览统计（总作品、总浏览、总点赞、总收藏） */
    private PostStatsVO summary;

    /** 月度趋势（按创建月份聚合浏览/点赞/作品数） */
    private List<MonthlyTrendVO> monthlyTrends;

    /** 每日趋势（近 30 天按日聚合） */
    private List<DailyTrendVO> dailyTrends;

    /** 板块分布 */
    private List<SectionDistributionVO> sectionDistribution;

    /** 浏览量 Top 5 作品 */
    private List<PostVO> topPosts;

    @Data
    public static class MonthlyTrendVO {
        /** 月份标签，如 "2026-07" */
        private String month;
        /** 当月新增作品数 */
        private int posts;
        /** 当月作品总浏览量 */
        private long views;
        /** 当月作品总点赞数 */
        private long likes;
    }

    @Data
    public static class DailyTrendVO {
        /** 日期标签，如 "07-22" */
        private String date;
        /** 当日新增作品数 */
        private int posts;
        /** 当日作品总浏览量 */
        private long views;
        /** 当日作品总点赞数 */
        private long likes;
    }

    @Data
    public static class SectionDistributionVO {
        /** 板块名称 */
        private String name;
        /** 该板块下的作品数 */
        private int count;
    }
}
