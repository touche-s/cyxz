package com.cyxz.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.post.entity.PostLikePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子点赞关系 Mapper
 */
@Mapper
public interface PostLikeMapper extends BaseMapper<PostLikePO> {
}
