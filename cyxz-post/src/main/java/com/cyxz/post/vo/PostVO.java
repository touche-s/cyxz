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

    /** 帖子类型：NORMAL / ARTICLE */
    private String postType;

    /** 圈子 ID */
    private Long circleId;

    /** 圈子名称 */
    private String circleName;

    /** 板块 ID */
    private Long sectionId;

    /** 板块名称 */
    private String sectionName;

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

    /** 帖子状态：0=草稿 1=待审核 2=已通过(公开) 3=拒绝 4=已删除 */
    private Integer status;

    /** 拒绝原因（仅 status=3 时有值） */
    private String reviewReason;

    /** 点赞数 */
    private Integer likes;

    /** 是否已点赞 */
    private Boolean liked;

    /** 评论数 */
    private Integer comments;

    /** 浏览数 */
    private Integer views;

    /** 收藏数 */
    private Integer collections;

    /** 是否已收藏 */
    private Boolean collected;

    /** 是否置顶 */
    private Boolean pinned;

    /** 置顶时间 */
    private LocalDateTime pinnedTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
