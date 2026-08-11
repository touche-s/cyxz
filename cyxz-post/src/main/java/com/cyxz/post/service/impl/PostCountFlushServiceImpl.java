package com.cyxz.post.service.impl;

import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.service.AbstractCountFlushService;
import com.cyxz.post.mapper.PostMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 帖子计数刷库服务实现
 * <p>定时将 Redis Hash 中的增量刷入 MySQL post 表。
 * <p>策略：遍历 Hash 所有 field，逐条 update，成功后扣减对应增量防止并发丢失。
 */
@Service
public class PostCountFlushServiceImpl extends AbstractCountFlushService implements com.cyxz.post.service.PostCountFlushService {

    private final PostMapper postMapper;

    public PostCountFlushServiceImpl(StringRedisTemplate stringRedisTemplate, PostMapper postMapper) {
        super(stringRedisTemplate);
        this.postMapper = postMapper;
    }

    /**
     * 刷浏览增量到 post.views
     * <p>浏览只增不减，delta <= 0 时跳过。
     */
    @Override
    public int flushViewCounts() {
        return flushDelta(CacheKeyConstants.POST_VIEW_DELTA, (postId, delta) -> {
            if (delta <= 0) return;
            postMapper.updateViews(postId, delta);
        });
    }

    /**
     * 刷点赞增量到 post.likes
     */
    @Override
    public int flushLikeCounts() {
        return flushDelta(CacheKeyConstants.POST_LIKE_DELTA, postMapper::updateLikes);
    }

    /**
     * 刷收藏增量到 post.collections
     */
    @Override
    public int flushCollectCounts() {
        return flushDelta(CacheKeyConstants.POST_COLLECT_DELTA, postMapper::updateCollections);
    }

    /**
     * 刷评论数增量到 post.comments
     */
    @Override
    public int flushCommentCounts() {
        return flushDelta(CacheKeyConstants.POST_COMMENT_DELTA, postMapper::updateComments);
    }
}
