package com.cyxz.post.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子实体
 * <p>对应 post 表，存储帖子的核心信息。
 */
@Data
@TableName("post")
public class PostPO extends BaseEntity {

    /** 帖子 ID（雪花算法） */
    @TableId
    private Long id;

    /** 作者 ID */
    private Long userId;

    /** 圈子 ID */
    private Long circleId;
    /** 板块 ID（circle_section 主键） */
    private Long sectionId;

    /** 帖子类型：NORMAL/ARTICLE */
    private String postType;

    /** 标题 */
    private String title;

    /** 正文内容 */
    private String content;

    /** 封面图 URL */
    private String cover;

    /** 图片列表 JSON */
    private String images;

    /** 标签 JSON 数组 */
    private String tags;

    /**
     * 帖子状态：0=草稿 1=待审核 2=已通过(公开) 3=拒绝 4=已删除
     * <p>状态流转链：草稿(0)→待审(1)→通过(2)/拒绝(3)；通过(2)→删除(4)；拒绝(3)→草稿(0)重新编辑
     */
    private Integer status;

    /** 拒绝原因（仅 status=3 时有值） */
    private String reviewReason;

    /** 点赞数 */
    private Integer likes;

    /** 评论数 */
    private Integer comments;

    /** 浏览数 */
    private Integer views;

    /** 收藏数 */
    private Integer collections;

    /** 是否置顶：0=否 1=是 */
    @TableField("is_pinned")
    private Integer pinned;

    /** 置顶时间 */
    private LocalDateTime pinnedTime;
}
