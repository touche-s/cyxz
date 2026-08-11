package com.cyxz.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.analytics.entity.DailyStatisticPO;
import com.cyxz.analytics.mapper.DailyStatisticMapper;
import com.cyxz.analytics.service.AnalyticsService;
import com.cyxz.analytics.vo.DashboardVO;
import com.cyxz.analytics.vo.TrendVO;
import com.cyxz.common.constant.AnalyticsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据统计服务实现
 * <p>基于 daily_statistic 表提供看板概览与单指标趋势查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final DailyStatisticMapper dailyStatisticMapper;

    /**
     * 获取看板概览（当日各指标汇总）
     * <p>查询当日全部指标记录，按指标类型映射到对应看板字段，缺失指标保持为 null。
     *
     * @return 当日各指标汇总
     */
    @Override
    public DashboardVO getDashboard() {
        LocalDate today = LocalDate.now();
        List<DailyStatisticPO> list = dailyStatisticMapper.selectList(new LambdaQueryWrapper<DailyStatisticPO>()
                .eq(DailyStatisticPO::getStatDate, today));
        DashboardVO vo = new DashboardVO();
        for (DailyStatisticPO po : list) {
            switch (po.getMetric()) {
                case AnalyticsConstants.METRIC_NEW_USER -> vo.setNewUsers(po.getValue());
                case AnalyticsConstants.METRIC_NEW_POST -> vo.setNewPosts(po.getValue());
                case AnalyticsConstants.METRIC_POST_APPROVED -> vo.setApprovedPosts(po.getValue());
                case AnalyticsConstants.METRIC_POST_REJECTED -> vo.setRejectedPosts(po.getValue());
                case AnalyticsConstants.METRIC_NEW_CIRCLE -> vo.setNewCircles(po.getValue());
                case AnalyticsConstants.METRIC_NEW_JOIN -> vo.setNewJoins(po.getValue());
                case AnalyticsConstants.METRIC_REPORT_HANDLED -> vo.setReportHandled(po.getValue());
                default -> log.warn("未知统计指标类型: {}", po.getMetric());
            }
        }
        return vo;
    }

    /**
     * 获取单指标趋势（近 N 天按日聚合）
     *
     * @param metric 统计指标类型
     * @param days   统计天数
     * @return 按日聚合的趋势列表
     */
    @Override
    public List<TrendVO> getTrend(String metric, int days) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays((long) days - 1);
        List<DailyStatisticPO> list = dailyStatisticMapper.selectList(new LambdaQueryWrapper<DailyStatisticPO>()
                .eq(DailyStatisticPO::getMetric, metric)
                .ge(DailyStatisticPO::getStatDate, start)
                .le(DailyStatisticPO::getStatDate, today)
                .orderByAsc(DailyStatisticPO::getStatDate));
        return list.stream().map(po -> {
            TrendVO vo = new TrendVO();
            vo.setDate(po.getStatDate());
            vo.setValue(po.getValue());
            return vo;
        }).toList();
    }
}
