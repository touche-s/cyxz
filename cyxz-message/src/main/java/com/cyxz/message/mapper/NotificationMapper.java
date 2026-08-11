package com.cyxz.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.message.entity.NotificationPO;

/**
 * 通知 Mapper
 * <p>对应 notification 表，存储用户消息通知记录，支持点赞、评论、回复、收藏、关注等类型。
 * <p>继承 MyBatis-Plus 的 BaseMapper，提供通知记录的通用增删改查能力。
 */
public interface NotificationMapper extends BaseMapper<NotificationPO> {
}
