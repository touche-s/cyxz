package com.cyxz.post.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建帖子请求
 */
@Data
public class CreatePostRequest {

    /** 圈子 ID */
    private Long circleId;

    /** 板块 ID */
    private Long sectionId;

    /** 标题（草稿时可为空，发布时必填） */
    private String title;

    /** 正文内容 */
    private String content;

    /** 封面图 URL */
    private String cover;

    /** 图片列表 */
    private List<String> images;

    /** 标签列表 */
    private List<String> tags;

    /** 帖子类型：NORMAL / ARTICLE，默认 NORMAL */
    private String postType;

    /** 状态：0=草稿 1=发布 */
    private Integer status;
}
