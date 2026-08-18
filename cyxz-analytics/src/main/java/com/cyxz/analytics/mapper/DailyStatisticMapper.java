package com.cyxz.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.analytics.entity.DailyStatisticPO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 每日统计 Mapper
 */
@Mapper
public interface DailyStatisticMapper extends BaseMapper<DailyStatisticPO> {

    /**
     * UPSERT 每日统计：利用 (stat_date, metric) 唯一约束，
     * 已存在则原子累加 value，不存在则插入。
     */
    @Insert("INSERT INTO daily_statistic (stat_date, metric, value, create_time, update_time) "
            + "VALUES (#{po.statDate}, #{po.metric}, #{po.value}, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE value = value + VALUES(value), update_time = NOW()")
    int upsert(@Param("po") DailyStatisticPO po);
}
