package com.cyxz.common;

public final class CacheKeyConstants {

    private CacheKeyConstants() {}

    public static final String POST_LIST_PREFIX = "post:list:";
    public static final String POST_DETAIL_PREFIX = "post:detail:";
    public static final String POST_HOT = "post:hot";
    public static final String HOT_POSTS_KEY = "post:hot:list";
    public static final String POST_HOT_TAGS = "post:hotTags";
    public static final String POST_LIST_KEYS_SET = "post:list:keys";
    public static final String POST_DETAIL_KEYS_SET = "post:detail:keys";

    public static final String USER_LOGIN_PREFIX = "user:login:";
    public static final String USER_LIKED_PREFIX = "user:liked:";
    public static final String USER_COLLECTED_PREFIX = "user:collected:";
    public static final String USER_LIKES_COUNT_PREFIX = "user:likesCount:";

    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    public static final long CACHE_EXPIRE_MINUTES = 30;
    public static final long CACHE_EXPIRE_JITTER_MINUTES = 10;
    public static final long USER_STATUS_EXPIRE_MINUTES = 5;

    public static String getUserLikedKey(Long userId, Long postId) {
        return USER_LIKED_PREFIX + userId + ":" + postId;
    }

    public static String getUserCollectedKey(Long userId, Long postId) {
        return USER_COLLECTED_PREFIX + userId + ":" + postId;
    }

    public static String getUserLoginKey(Long userId) {
        return USER_LOGIN_PREFIX + userId;
    }

    public static String getTokenBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
    }

    public static String getPostDetailKey(Long postId) {
        return POST_DETAIL_PREFIX + postId;
    }

    public static String getUserLikesCountKey(Long userId) {
        return USER_LIKES_COUNT_PREFIX + userId;
    }

    public static long getExpireMinutesWithJitter() {
        return CACHE_EXPIRE_MINUTES + (long) (Math.random() * CACHE_EXPIRE_JITTER_MINUTES);
    }
}
