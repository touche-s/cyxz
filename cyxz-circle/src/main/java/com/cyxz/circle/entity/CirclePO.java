package com.cyxz.circle.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

/**
 * 圈子实体，对应 circle 表
 */
@Data
@TableName("circle")
public class CirclePO extends BaseEntity {

    @TableId
    private Long id;

    private String name;

    private String slug;

    private String intro;

    private String avatar;

    private String cover;

    private Integer status;

    private Integer sortOrder;

    private Integer postCount;

    private Integer memberCount;
}
