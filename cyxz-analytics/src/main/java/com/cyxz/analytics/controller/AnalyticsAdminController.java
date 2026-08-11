package com.cyxz.analytics.controller;

import com.cyxz.analytics.service.AnalyticsService;
import com.cyxz.analytics.vo.DashboardVO;
import com.cyxz.analytics.vo.TrendVO;
import com.cyxz.common.base.Result;
import com.cyxz.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据统计看板控制器（平台管理员）
 * <p>提供看板概览与单指标趋势查询，权限码暂未接入，仅校验登录态。
 */
@Tag(name = "数据统计看板（平台管理员）")
@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsAdminController {

    private final AnalyticsService analyticsService;

    /**
     * 看板概览（当日各指标汇总）
     *
     * @param userId 当前登录用户 ID
     * @return 当日各指标汇总
     */
    @Operation(summary = "看板概览")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(@CurrentUser Long userId) {
        return Result.success(analyticsService.getDashboard());
    }

    /**
     * 单指标趋势（近 N 天按日聚合）
     *
     * @param metric 统计指标类型
     * @param days   统计天数（默认 7）
     * @param userId 当前登录用户 ID
     * @return 按日聚合的趋势列表
     */
    @Operation(summary = "单指标趋势")
    @GetMapping("/trend")
    public Result<List<TrendVO>> trend(@RequestParam("metric") String metric,
                                        @RequestParam(value = "days", defaultValue = "7") int days,
                                        @CurrentUser Long userId) {
        return Result.success(analyticsService.getTrend(metric, days));
    }
}
