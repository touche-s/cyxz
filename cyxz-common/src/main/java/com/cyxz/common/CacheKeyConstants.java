package com.cyxz.common;

/**
 * 缓存 Key 常量
 * <p>统一定义 Redis 缓存 Key 前缀及过期时间配置
 */
public final class CacheKeyConstants {

    private CacheKeyConstants() {
    }

    // === 帖子缓存 ===
    public static final String POST_LIST_PREFIX = "post:list:";
    public static final String POST_DETAIL_PREFIX = "post:detail:";
    public static final String POST_HOT = "post:hot";
    public static final String HOT_POSTS_KEY = "post:hot:list";
    public static final String POST_HOT_TAGS = "post:hotTags";
    public static final String POST_LIST_KEYS_SET = "post:list:keys";
    public static final String POST_DETAIL_KEYS_SET = "post:detail:keys";

    // === 用户缓存 ===
    public static final String USER_LOGIN_PREFIX = "user:login:";
    public static final String USER_LIKED_PREFIX = "user:liked:";
    public static final String USER_COLLECTED_PREFIX = "user:collected:";
    public static final String USER_LIKES_COUNT_PREFIX = "user:likesCount:";

    // === Token 黑名单 ===
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    // === 过期时间配置（分钟） ===
    public static final long CACHE_EXPIRE_MINUTES = 30;
    public static final long CACHE_EXPIRE_JITTER_MINUTES = 10;
    public static final long USER_STATUS_EXPIRE_MINUTES = 5;

    /**
     * 获取用户点赞 Key
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return Redis Key
     */
    public static String getUserLikedKey(Long userId, Long postId) {
        return USER_LIKED_PREFIX + userId + ":" + postId;
    }

    /**
     * 获取用户收藏 Key
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return Redis Key
     */
    public static String getUserCollectedKey(Long userId, Long postId) {
        return USER_COLLECTED_PREFIX + userId + ":" + postId;
    }

    /**
     * 获取用户登录缓存 Key
     *
     * @param userId 用户ID
     * @return Redis Key
     */
    public static String getUserLoginKey(Long userId) {
        return USER_LOGIN_PREFIX + userId;
    }

    /**
     * 获取 Token 黑名单 Key
     *
     * @param token Token
     * @return Redis Key
     */
    public static String getTokenBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
    }

    /**
     * 获取帖子详情缓存 Key
     *
     * @param postId 帖子ID
     * @return Redis Key
     */
    public static String getPostDetailKey(Long postId) {
        return POST_DETAIL_PREFIX + postId;
    }

    /**
     * 获取用户获赞数缓存 Key
     *
     * @param userId 用户ID
     * @return Redis Key
     */
    public static String getUserLikesCountKey(Long userId) {
        return USER_LIKES_COUNT_PREFIX + userId;
    }

    /**
     * 获取带随机抖动的过期时间（防止缓存雪崩）
     *
     * @return 过期时间（分钟）
     */
    public static long getExpireMinutesWithJitter() {
        return CACHE_EXPIRE_MINUTES + (long) (Math.random() * CACHE_EXPIRE_JITTER_MINUTES);
    }
}
