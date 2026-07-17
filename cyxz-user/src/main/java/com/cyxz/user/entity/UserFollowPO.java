package com.cyxz.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_follow")
public class UserFollowPO {

    @TableId
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("follow_user_id")
    private Long followUserId;

    @TableField("status")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}