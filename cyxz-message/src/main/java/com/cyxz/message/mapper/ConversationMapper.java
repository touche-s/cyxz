package com.cyxz.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.message.entity.ConversationPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 私信会话 Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationPO> {
}
