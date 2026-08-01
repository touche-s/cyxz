package com.cyxz.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.message.entity.PrivateMessagePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 私信消息 Mapper
 */
@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessagePO> {
}
