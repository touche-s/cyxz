package com.cyxz.common.constant;

/**
 * 缓存 Key 常量
 * <p>统一定义 Redis 缓存 Key 前缀、过期时间配置及辅助方法。
 * <p>所有 Key 自动加环境命名空间前缀（默认 cyxz:dev），多环境共享 Redis 时避免 key 冲突。
 * 可通过 JVM 参数 {@code -Dredis.namespace=cyxz:prod} 覆盖。
 */
public final class CacheKeyConstants {

    private CacheKeyConstants() {
    }

    /** 环境命名空间，可通过 -Dredis.namespace=xxx 覆盖 */
    private static final String NS = System.getProperty("redis.namespace", "cyxz:dev") + ":";

    /** 帖子列表缓存前缀 */
    public static final String POST_LIST_PREFIX = NS + "post:list:";

    /** 帖子详情缓存前缀 */
    public static final String POST_DETAIL_PREFIX = NS + "post:detail:";

    /** 热门帖子集合 */
    public static final String POST_HOT = NS + "post:hot";

    /** 热门帖子列表 */
    public static final String HOT_POSTS_KEY = NS + "post:hot:list";

    /** 热门标签 */
    public static final String POST_HOT_TAGS = NS + "post:hotTags";

    /** 帖子列表缓存 Key 集合（用于批量管理） */
    public static final String POST_LIST_KEYS_SET = NS + "post:list:keys";

    /** 帖子详情缓存 Key 集合（用于批量管理） */
    public static final String POST_DETAIL_KEYS_SET = NS + "post:detail:keys";

    /** 用户点赞缓存前缀 */
    public static final String USER_LIKED_PREFIX = NS + "user:liked:";

    /** 用户点赞帖子集合前缀 */
    public static final String USER_LIKED_POSTS = NS + "user:liked:posts:";

    /** 用户点赞评论集合前缀 */
    public static final String USER_LIKED_COMMENTS = NS + "user:liked:comments:";

    /** 用户收藏缓存前缀 */
    public static final String USER_COLLECTED_PREFIX = NS + "user:collected:";

    /** 用户获赞数缓存前缀 */
    public static final String USER_LIKES_COUNT_PREFIX = NS + "user:likesCount:";

    /** 帖子浏览增量 Hash（field=postId, value=增量） */
    public static final String POST_VIEW_DELTA = NS + "post:view:delta";

    /** 帖子点赞增量 Hash（field=postId, value=增量） */
    public static final String POST_LIKE_DELTA = NS + "post:like:delta";

    /** 帖子收藏增量 Hash（field=postId, value=增量） */
    public static final String POST_COLLECT_DELTA = NS + "post:collect:delta";

    /** 帖子评论数增量 Hash（field=postId, value=增量） */
    public static final String POST_COMMENT_DELTA = NS + "post:comment:delta";

    /** 帖子评论总数缓存（直接存绝对数值，用于翻页时替代 COUNT） */
    public static final String POST_COMMENT_COUNT_PREFIX = NS + "post:comment:count:";

    /** 评论点赞增量 Hash（field=commentId, value=增量） */
    public static final String COMMENT_LIKE_DELTA = NS + "comment:like:delta";

    /** 帖子浏览去重前缀（post:view:dedup:{postId}:{userOrIp}） */
    public static final String POST_VIEW_DEDUP_PREFIX = NS + "post:view:dedup:";

    /** 浏览去重过期时间（分钟） */
    public static final long POST_VIEW_DEDUP_MINUTES = 30;

    /** Token 黑名单前缀 */
    public static final String TOKEN_BLACKLIST_PREFIX = NS + "token:blacklist:";

    /** 全局权限缓存前缀（auth:global:{userId}） */
    public static final String AUTH_GLOBAL_PREFIX = NS + "auth:global:";

    /** 圈子权限缓存前缀（auth:circle:{userId}:{circleId}） */
    public static final String AUTH_CIRCLE_PREFIX = NS + "auth:circle:";

    /** 图形验证码缓存前缀 */
    public static final String CAPTCHA_PREFIX = NS + "captcha:";

    /** 图形验证码过期时间（分钟） */
    public static final long CAPTCHA_EXPIRE_MINUTES = 5;

    /** 登录失败计数前缀（login:fail:{ip}） */
    public static final String LOGIN_FAIL_PREFIX = NS + "login:fail:";

    /** 登录失败计数窗口（分钟） */
    public static final long LOGIN_FAIL_WINDOW_MINUTES = 5;

    /** 登录失败最大次数，超过则锁定 */
    public static final int LOGIN_FAIL_MAX_ATTEMPTS = 10;

    /** 防重复提交 Key 前缀（prevent:repeat:{userId}:{uri}:{argsHash}） */
    public static final String PREVENT_REPEAT_PREFIX = NS + "prevent:repeat:";

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
     * 获取全局权限缓存 Key
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String getAuthGlobalKey(Long userId) {
        return AUTH_GLOBAL_PREFIX + userId;
    }

    /**
     * 获取圈子权限缓存 Key
     *
     * @param userId   用户 ID
     * @param circleId 圈子 ID
     * @return Redis Key
     */
    public static String getAuthCircleKey(Long userId, Long circleId) {
        return AUTH_CIRCLE_PREFIX + userId + ":" + circleId;
    }

    /**
     * 获取某用户所有圈子权限缓存的通配 Key（用于登出/权限变更时批量删除）
     *
     * @param userId 用户 ID
     * @return Redis Key 模式（auth:circle:{userId}:*）
     */
    public static String getAuthCirclePattern(Long userId) {
        return AUTH_CIRCLE_PREFIX + userId + ":*";
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

    /**
     * 获取登录失败计数 Key
     *
     * @param ip 客户端 IP
     * @return Redis Key
     */
    public static String getLoginFailKey(String ip) {
        return LOGIN_FAIL_PREFIX + ip;
    }

    /**
     * 获取帖子浏览去重 Key
     *
     * @param postId   帖子 ID
     * @param identity 用户 ID 或 IP 组成的身份标识（如 "user:123" / "ip:1.2.3.4"）
     * @return Redis Key
     */
    public static String getPostViewDedupKey(Long postId, String identity) {
        return POST_VIEW_DEDUP_PREFIX + postId + ":" + identity;
    }
}
