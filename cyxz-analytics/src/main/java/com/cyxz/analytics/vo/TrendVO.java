package com.cyxz.analytics.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 指标趋势 VO（单指标按日聚合）
 */
@Data
public class TrendVO {

    /** 统计日期 */
    private LocalDate date;

    /** 指标当日值 */
    private Integer value;
}
