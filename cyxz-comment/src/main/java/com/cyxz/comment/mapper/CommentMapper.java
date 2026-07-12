package com.cyxz.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.comment.entity.CommentPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentPO> {
}
