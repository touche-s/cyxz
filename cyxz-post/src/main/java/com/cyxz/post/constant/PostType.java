package com.cyxz.post.constant;

/**
 * 帖子类型常量
 * <p>仅 post 服务内部使用，避免类型字符串硬编码导致不一致。
 * <p>NORMAL=图文帖（至少一张图片） ARTICLE=长文帖（正文至少 100 字）
 */
public final class PostType {

    private PostType() {}

    /** 图文帖 */
    public static final String NORMAL = "NORMAL";

    /** 长文帖 */
    public static final String ARTICLE = "ARTICLE";
}
