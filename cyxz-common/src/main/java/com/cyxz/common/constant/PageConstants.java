package com.cyxz.common.constant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页常量
 */
public final class PageConstants {

    private PageConstants() {}

    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认分页大小（帖子列表） */
    public static final int DEFAULT_SIZE = 10;

    /** 评论/用户分页大小 */
    public static final int SIZE_20 = 20;

    /** 子回复/排行榜分页大小 */
    public static final int SIZE_5 = 5;

    /** 分页参数默认值字符串（用于 @RequestParam defaultValue） */
    public static final String DEFAULT_PAGE_STR = "1";
    public static final String DEFAULT_SIZE_STR = "10";
    public static final String SIZE_20_STR = "20";
    public static final String SIZE_5_STR = "5";

    /**
     * 创建 MyBatis-Plus 分页对象
     */
    public static <T> Page<T> pageOf(int page, int size) {
        return new Page<>(page, size);
    }
}
