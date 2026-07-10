package com.cyxz.post.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子视图对象
 */
@Data
public class PostVO {

    /** 帖子 ID */
    private Long id;

    /** 作者 ID */
    private Long userId;

    /** 作者昵称 */
    private String authorName;

    /** 作者头像 */
    private String authorAvatar;

    /** 分类 ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 标题 */
    private String title;

    /** 正文内容 */
    private String content;

    /** 封面图 URL */
    private String cover;

    /** 图片列表 */
    private List<String> images;

    /** 标签列表 */
    private List<String> tags;

    /** 状态：0=草稿 1=已发布 2=已删除 */
    private Integer status;

    /** 点赞数 */
    private Integer likes;

    /** 评论数 */
    private Integer comments;

    /** 浏览数 */
    private Integer views;

    /** 收藏数 */
    private Integer collections;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
