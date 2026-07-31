package com.cyxz.circle.vo;

import lombok.Data;

/**
 * 圈子展示 VO
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
