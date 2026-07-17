package com.cyxz.post.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类实体
 * <p>对应 category 表，存储帖子分类信息。
 */
@Data
@TableName("category")
public class CategoryPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类 ID（雪花算法） */
    @TableId
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类描述 */
    private String description;

    /** 排序值（越小越靠前） */
    private Integer sortOrder;

    /** 状态：0=禁用 1=启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
