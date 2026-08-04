package com.cyxz.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.message.entity.ConversationPO;

/**
 * 私信会话 Mapper
 * <p>对应 conversation 表，记录两个用户之间的对话关系，约定 user_id_1 < user_id_2 以避免重复会话。
 * <p>继承 MyBatis-Plus 的 BaseMapper，提供会话记录的通用增删改查能力。
 */
public interface ConversationMapper extends BaseMapper<ConversationPO> {
}
