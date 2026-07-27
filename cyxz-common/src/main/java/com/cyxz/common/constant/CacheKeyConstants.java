package com.cyxz.common.constant;

/**
 * 缓存 Key 常量
 * <p>统一定义 Redis 缓存 Key 前缀、过期时间配置及辅助方法。
 */
public final class CacheKeyConstants {

    private CacheKeyConstants() {
    }

    /** 帖子列表缓存前缀 */
    public static final String POST_LIST_PREFIX = "post:list:";

    /** 帖子详情缓存前缀 */
    public static final String POST_DETAIL_PREFIX = "post:detail:";

    /** 热门帖子集合 */
    public static final String POST_HOT = "post:hot";

    /** 热门帖子列表 */
    public static final String HOT_POSTS_KEY = "post:hot:list";

    /** 热门标签 */
    public static final String POST_HOT_TAGS = "post:hotTags";

    /** 帖子列表缓存 Key 集合（用于批量管理） */
    public static final String POST_LIST_KEYS_SET = "post:list:keys";

    /** 帖子详情缓存 Key 集合（用于批量管理） */
    public static final String POST_DETAIL_KEYS_SET = "post:detail:keys";

    /** 用户点赞缓存前缀 */
    public static final String USER_LIKED_PREFIX = "user:liked:";

    /** 用户点赞帖子集合前缀 */
    public static final String USER_LIKED_POSTS = "user:liked:posts:";

    /** 用户点赞评论集合前缀 */
    public static final String USER_LIKED_COMMENTS = "user:liked:comments:";

    /** 用户收藏缓存前缀 */
    public static final String USER_COLLECTED_PREFIX = "user:collected:";

    /** 用户获赞数缓存前缀 */
    public static final String USER_LIKES_COUNT_PREFIX = "user:likesCount:";

    /** 帖子浏览增量 Hash（field=postId, value=增量） */
    public static final String POST_VIEW_DELTA = "post:view:delta";

    /** 帖子点赞增量 Hash（field=postId, value=增量） */
    public static final String POST_LIKE_DELTA = "post:like:delta";

    /** 帖子收藏增量 Hash（field=postId, value=增量） */
    public static final String POST_COLLECT_DELTA = "post:collect:delta";

    /** 帖子评论数增量 Hash（field=postId, value=增量） */
    public static final String POST_COMMENT_DELTA = "post:comment:delta";

    /** 评论点赞增量 Hash（field=commentId, value=增量） */
    public static final String COMMENT_LIKE_DELTA = "comment:like:delta";

    /** 圈子帖子数增量 Hash（field=circleId, value=增量，由 cyxz-post 写入，cyxz-circle 消费刷库） */
    public static final String CIRCLE_POST_DELTA = "circle:post:delta";

    /** 帖子浏览去重前缀（post:view:dedup:{postId}:{userOrIp}） */
    public static final String POST_VIEW_DEDUP_PREFIX = "post:view:dedup:";

    /** 浏览去重过期时间（分钟） */
    public static final long POST_VIEW_DEDUP_MINUTES = 30;

    /** Token 黑名单前缀 */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** 图形验证码缓存前缀 */
    public static final String CAPTCHA_PREFIX = "captcha:";

    /** 图形验证码过期时间（分钟） */
    public static final long CAPTCHA_EXPIRE_MINUTES = 5;

    /**
     * 获取用户点赞 Key
     *
     * @param userId 用户 ID
     * @param postId 帖子 ID
     * @return Redis Key
     */
    public static String getUserLikedKey(Long userId, Long postId) {
        return USER_LIKED_PREFIX + userId + ":" + postId;
    }

    /**
     * 获取用户收藏 Key
     *
     * @param userId 用户 ID
     * @param postId 帖子 ID
     * @return Redis Key
     */
    public static String getUserCollectedKey(Long userId, Long postId) {
        return USER_COLLECTED_PREFIX + userId + ":" + postId;
    }

    /**
     * 获取 Token 黑名单 Key
     *
     * @param jti JWT ID（Token 唯一标识）
     * @return Redis Key
     */
    public static String getTokenBlacklistKey(String jti) {
        return TOKEN_BLACKLIST_PREFIX + jti;
    }

    /**
     * 获取帖子详情缓存 Key
     *
     * @param postId 帖子 ID
     * @return Redis Key
     */
    public static String getPostDetailKey(Long postId) {
        return POST_DETAIL_PREFIX + postId;
    }

    /**
     * 获取验证码缓存 Key
     *
     * @param uuid 验证码唯一标识
     * @return Redis Key
     */
    public static String getCaptchaKey(String uuid) {
        return CAPTCHA_PREFIX + uuid;
    }

    /**
     * 获取用户获赞数缓存 Key
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String getUserLikesCountKey(Long userId) {
        return USER_LIKES_COUNT_PREFIX + userId;
    }
}
