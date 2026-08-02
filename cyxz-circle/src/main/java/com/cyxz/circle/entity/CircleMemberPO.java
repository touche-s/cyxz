package com.cyxz.circle.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

/**
 * 圈子成员关系实体，对应 circle_member 表
 */
@Data
@TableName("circle_member")
public class CircleMemberPO extends BaseEntity {

    @TableId
    private Long id;

    private Long circleId;

    private Long userId;

    private Integer status;
}
