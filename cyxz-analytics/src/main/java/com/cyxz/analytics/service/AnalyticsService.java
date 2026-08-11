package com.cyxz.analytics.service;

import com.cyxz.analytics.vo.DashboardVO;
import com.cyxz.analytics.vo.TrendVO;

import java.util.List;

/**
 * 数据统计服务
 */
public interface AnalyticsService {

    /**
     * 获取看板概览（当日各指标汇总）
     *
     * @return 当日各指标汇总
     */
    DashboardVO getDashboard();

    /**
     * 获取单指标趋势（近 N 天按日聚合）
     *
     * @param metric 统计指标类型
     * @param days   统计天数
     * @return 按日聚合的趋势列表
     */
    List<TrendVO> getTrend(String metric, int days);
}
