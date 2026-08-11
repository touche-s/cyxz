package com.cyxz.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.analytics.entity.DailyStatisticPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日统计 Mapper
 */
@Mapper
public interface DailyStatisticMapper extends BaseMapper<DailyStatisticPO> {
}
