package com.cyxz.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.comment.entity.CommentLikePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论点赞关系 Mapper
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLikePO> {
}
