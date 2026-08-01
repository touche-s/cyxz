package com.cyxz.post.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

/**
 * 帖子收藏关系实体
 * <p>对应 post_collect 表，存储用户与帖子的收藏关系。
 * 采用逻辑状态型：status=1 已收藏，status=0 已取消。
 */
@Data
@TableName("post_collect")
public class PostCollectPO extends BaseEntity {

    /** 主键 */
    @TableId
    private Long id;

    /** 帖子 ID */
    private Long postId;

    /** 用户 ID */
    private Long userId;

    /** 状态：1=已收藏 0=已取消 */
    private Integer status;
}
