package com.cyxz.circle.vo;

import lombok.Data;

/**
 * 圈子板块展示 VO，用于前端展示圈子已启用的板块
 * <p>name 和 applicableType 来自关联的 section_template 表，而非本表
 */
@Data
public class CircleSectionVO {

    /** circle_section 表主键 */
    private Long id;
    /** 所属圈子 ID */
    private Long circleId;
    /** 关联的模板 ID */
    private Long templateId;
    /** 板块名称，来自 section_template.name */
    private String name;
    /** 适用类型，来自 section_template.applicable_type。ALL/NORMAL/ARTICLE */
    private String applicableType;
    /** 是否默认板块：1=是 */
    private Integer isDefault;
    /** 排序值，数字越小越靠前 */
    private Integer sortOrder;
    /** 板块状态：1=启用 */
    private Integer status;
}
