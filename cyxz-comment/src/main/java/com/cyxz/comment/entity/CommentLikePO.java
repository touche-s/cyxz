package com.cyxz.comment.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

/**
 * 评论点赞关系实体
 * <p>对应 comment_like 表，存储用户与评论的点赞关系。
 * 采用逻辑状态型：status=1 已点赞，status=0 已取消。
 */
@Data
@TableName("comment_like")
public class CommentLikePO extends BaseEntity {

    /** 主键 */
    @TableId
    private Long id;

    /** 评论 ID */
    private Long commentId;

    /** 用户 ID */
    private Long userId;

    /** 状态：1=已点赞 0=已取消 */
    private Integer status;
}
