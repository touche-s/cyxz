package com.cyxz.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.message.entity.PrivateMessagePO;

/**
 * 私信消息 Mapper
 * <p>对应 private_message 表，存储每条私信的详细内容，与会话表 conversation 关联。
 * <p>继承 MyBatis-Plus 的 BaseMapper，提供私信消息的通用增删改查能力。
 */
public interface PrivateMessageMapper extends BaseMapper<PrivateMessagePO> {
}
