package com.cyxz.comment.service.impl;

import com.cyxz.comment.mapper.CommentMapper;
import com.cyxz.comment.service.CommentCountFlushService;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.service.AbstractCountFlushService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 评论计数刷库服务实现
 * <p>定时将 Redis Hash 评论点赞增量刷入 MySQL comment.likes。
 */
@Service
public class CommentCountFlushServiceImpl extends AbstractCountFlushService implements CommentCountFlushService {

    private final CommentMapper commentMapper;

    public CommentCountFlushServiceImpl(StringRedisTemplate stringRedisTemplate, CommentMapper commentMapper) {
        super(stringRedisTemplate);
        this.commentMapper = commentMapper;
    }

    /**
     * 刷评论点赞增量到 comment.likes
     */
    @Override
    public int flushLikeCounts() {
        return flushDelta(CacheKeyConstants.COMMENT_LIKE_DELTA, commentMapper::updateLikes);
    }
}
