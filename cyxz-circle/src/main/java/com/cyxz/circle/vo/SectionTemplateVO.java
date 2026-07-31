package com.cyxz.circle.vo;

import lombok.Data;

/**
 * 板块模板展示 VO，用于管理后台展示所有可用的板块模板
 */
@Data
public class SectionTemplateVO {

    private Long id;
    /** 板块名称，如"图楼分享""作品讨论""同人创作" */
    private String name;
    /** 适用类型：ALL=全部，NORMAL=图文帖，ARTICLE=长文 */
    private String applicableType;
    /** 模板描述 */
    private String description;
    /** 排序值，数字越小越靠前 */
    private Integer sortOrder;
}
