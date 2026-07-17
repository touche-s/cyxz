package com.cyxz.post.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帖子实体
 * <p>对应 post 表，存储帖子的核心信息。
 */
@Data
@TableName("post")
public class PostPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 帖子 ID（雪花算法） */
    @TableId
    private Long id;

    /** 作者 ID */
    private Long userId;

    /** 分类 ID */
    private Long categoryId;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
