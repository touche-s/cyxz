package com.cyxz.post.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 更新帖子请求
 */
@Data
public class UpdatePostRequest {

    /** 帖子 ID（前端传 String 避免 JS 精度丢失） */
    @NotBlank(message = "帖子ID不能为空")
    private String id;

    /** 分类 ID */
    private Long categoryId;

    /** 圈子 ID */
    private Long circleId;

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

    /**
     * 解析帖子 ID 为 Long，供业务层使用。
     */
    public Long getIdAsLong() {
        return Long.parseLong(id.trim());
    }
}
