package com.cyxz.circle.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 圈子板块关联实体，对应 circle_section 表
 * <p>记录某个圈子启用了哪些板块模板，以及板块在该圈子内的个性化配置（是否默认、排序、状态）
 */
@Data
@TableName("circle_section")
public class CircleSectionPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /** 圈子 ID */
    private Long circleId;
    /** 关联的板块模板 ID */
    private Long templateId;
    /** 是否默认板块：1=是，0=否。发帖时默认选中 */
    private Integer isDefault;
    /** 圈子内排序，数字越小越靠前 */
    private Integer sortOrder;
    /** 状态：1=启用，0=禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
