package com.cyxz.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.post.entity.PostCollectPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子收藏关系 Mapper
 */
@Mapper
public interface PostCollectMapper extends BaseMapper<PostCollectPO> {
}
