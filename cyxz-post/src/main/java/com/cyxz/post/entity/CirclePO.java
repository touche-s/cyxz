package com.cyxz.post.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 圈子实体
 */
@Data
@TableName("circle")
public class CirclePO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /** 圈子名称 */
    private String name;

    /** URL 友好标识 */
    private String slug;

    /** 一句话简介 */
    private String intro;

    /** 头像 URL */
    private String avatar;

    /** 封面 URL */
    private String cover;

    /** 状态：0=禁用 1=启用 */
    private Integer status;

    /** 排序值 */
    private Integer sortOrder;

    /** 帖子数 */
    private Integer postCount;

    /** 成员数 */
    private Integer memberCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
