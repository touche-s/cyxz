package com.cyxz.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

/**
 * 用户关注关系实体，对应 user_follow 表
 * <p>采用逻辑状态型：status=1 已关注，status=0 已取消。
 */
@Data
@TableName("user_follow")
public class UserFollowPO extends BaseEntity {

    @TableId
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("follow_user_id")
    private Long followUserId;

    @TableField("status")
    private Integer status;
}
