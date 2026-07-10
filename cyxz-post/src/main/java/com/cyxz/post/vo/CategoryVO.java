package com.cyxz.post.vo;

import lombok.Data;

/**
 * 分类视图对象
 */
@Data
public class CategoryVO {

    /** 分类 ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类描述 */
    private String description;

    /** 排序值 */
    private Integer sortOrder;
}
