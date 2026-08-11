package com.cyxz.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 每日统计实体，对应 daily_statistic 表
 * <p>按 (stat_date, metric) 唯一约束聚合每日各指标值，由消费者 UPSERT 写入。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_statistic")
public class DailyStatisticPO extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 统计日期 */
    private LocalDate statDate;

    /** 统计指标类型，取值见 {@link com.cyxz.common.constant.AnalyticsConstants} 的 METRIC_* 常量 */
    private String metric;

    /** 指标累计值 */
    private Integer value;
}
