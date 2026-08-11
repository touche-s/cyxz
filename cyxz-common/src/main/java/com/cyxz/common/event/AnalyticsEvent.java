package com.cyxz.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 数据统计事件
 * <p>各业务服务（用户/帖子/圈子/治理等）产生统计指标后发布到 RabbitMQ，
 * 由 cyxz-analytics 服务消费并落库到每日统计表，实现统计数据的异步聚合。
 *
 * <pre>{@code
 * 业务事件发生（如新用户注册、帖子发布、举报处理）
 *      ↓ 发布 AnalyticsEvent
 * analytics 消费 → UPSERT daily_statistic 表
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 统计指标类型，取值见 {@link com.cyxz.common.constant.AnalyticsConstants} 的 METRIC_* 常量 */
    private String metric;

    /** 指标增量值 */
    private Integer value;

    /** 统计日期（null 时由消费端按当天补齐） */
    private LocalDate statDate;
}
