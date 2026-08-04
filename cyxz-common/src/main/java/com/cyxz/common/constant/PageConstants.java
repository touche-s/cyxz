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

    /** 分页大小上限，防止恶意大分页请求拖垮数据库 */
    public static final int MAX_SIZE = 50;

    /** 分页参数默认值字符串（用于 @RequestParam defaultValue） */
    public static final String DEFAULT_PAGE_STR = "1";
    public static final String DEFAULT_SIZE_STR = "10";
    public static final String SIZE_20_STR = "20";
    public static final String SIZE_5_STR = "5";

    /**
     * 创建 MyBatis-Plus 分页对象
     * <p>对 page/size 做边界校验：page 小于 1 归正为 1，size 超出 [1, MAX_SIZE] 截断到合法范围。
     */
    public static <T> Page<T> pageOf(int page, int size) {
        if (page < 1) {
            page = DEFAULT_PAGE;
        }
        if (size < 1) {
            size = DEFAULT_SIZE;
        } else if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        return new Page<>(page, size);
    }
}
