package com.cyxz.post.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(max = 50, message = "标题最长50字")
    private String title;

    /** 正文内容 */
    @Size(max = 50000, message = "正文过长")
    private String content;

    /** 封面图 URL */
    private String cover;

    /** 图片列表 */
    @Size(max = 9, message = "最多9张图片")
    private List<String> images;

    /** 标签列表 */
    @Size(max = 5, message = "最多5个标签")
    private List<String> tags;

    /** 帖子类型：NORMAL / ARTICLE，默认 NORMAL */
    @Pattern(regexp = "NORMAL|ARTICLE", message = "帖子类型只能是 NORMAL 或 ARTICLE")
    private String postType;

    /** 状态：0=草稿 1=发布 */
    private Integer status;
}
