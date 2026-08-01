package com.cyxz.circle.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyxz.common.base.BaseEntity;
import lombok.Data;

/**
 * 板块模板实体，对应 section_template 表
 * <p>模板是系统中所有板块的标准化定义，各圈子通过 circle_section 关联模板来启用板块
 */
@Data
@TableName("section_template")
public class SectionTemplatePO extends BaseEntity {

    @TableId
    private Long id;

    /** 板块名称，如"图楼分享""作品讨论""同人创作" */
    private String name;
    /** 适用内容类型：ALL=全部，NORMAL=图文帖，ARTICLE=长文 */
    private String applicableType;
    /** 模板描述 */
    private String description;
    /** 排序值，数字越小越靠前 */
    private Integer sortOrder;
}
