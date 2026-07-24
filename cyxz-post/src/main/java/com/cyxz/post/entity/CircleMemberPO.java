package com.cyxz.post.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 圈子成员实体
 */
@Data
@TableName("circle_member")
public class CircleMemberPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /** 圈子 ID */
    private Long circleId;

    /** 用户 ID */
    private Long userId;

    /** 状态：1=已加入 0=已退出 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
