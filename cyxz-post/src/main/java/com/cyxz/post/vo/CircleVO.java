package com.cyxz.post.vo;

import lombok.Data;

/**
 * 圈子视图对象
 */
@Data
public class CircleVO {

    private Long id;
    private String name;
    private String slug;
    private String intro;
    private String avatar;
    private String cover;
    private Integer postCount;
    private Integer memberCount;
    private Boolean joined;
}
