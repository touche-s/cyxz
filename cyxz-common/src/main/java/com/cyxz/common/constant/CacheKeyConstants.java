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

    /** 用户登录缓存前缀 */
    public static final String USER_LOGIN_PREFIX = "user:login:";

    /** 用户点赞缓存前缀 */
    public static final String USER_LIKED_PREFIX = "user:liked:";

    /** 用户收藏缓存前缀 */
    public static final String USER_COLLECTED_PREFIX = "user:collected:";

    /** 用户获赞数缓存前缀 */
    public static final String USER_LIKES_COUNT_PREFIX = "user:likesCount:";

    /** Token 黑名单前缀 */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** 默认缓存过期时间（分钟） */
    public static final long CACHE_EXPIRE_MINUTES = 30;

    /** 缓存过期随机抖动上限（分钟），防止缓存雪崩 */
    public static final long CACHE_EXPIRE_JITTER_MINUTES = 10;

    /** 用户状态缓存过期时间（分钟） */
    public static final long USER_STATUS_EXPIRE_MINUTES = 5;

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
     * 获取用户登录缓存 Key
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String getUserLoginKey(Long userId) {
        return USER_LOGIN_PREFIX + userId;
    }

    /**
     * 获取 Token 黑名单 Key
     *
     * @param token JWT Token 字符串
     * @return Redis Key
     */
    public static String getTokenBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
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
     * 获取用户获赞数缓存 Key
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String getUserLikesCountKey(Long userId) {
        return USER_LIKES_COUNT_PREFIX + userId;
    }

    /**
     * 获取带随机抖动的过期时间，防止缓存雪崩
     *
     * @return 过期时间（分钟）
     */
    public static long getExpireMinutesWithJitter() {
        return CACHE_EXPIRE_MINUTES + (long) (Math.random() * CACHE_EXPIRE_JITTER_MINUTES);
    }
}
