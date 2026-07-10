package com.cyxz.post.dto;

import lombok.Data;

import java.util.List;

/**
 * 更新帖子请求
 */
@Data
public class UpdatePostRequest {

    /** 帖子 ID */
    private Long id;

    /** 分类 ID */
    private Long categoryId;

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

    /** 状态：0=草稿 1=发布 2=删除 */
    private Integer status;
}
