package com.cyxz.post.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帖子点赞关系实体
 * <p>对应 post_like 表，存储用户与帖子的点赞关系。
 * 采用逻辑状态型：status=1 已点赞，status=0 已取消。
 */
@Data
@TableName("post_like")
public class PostLikePO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId
    private Long id;

    /** 帖子 ID */
    private Long postId;

    /** 用户 ID */
    private Long userId;

    /** 状态：1=已点赞 0=已取消 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
