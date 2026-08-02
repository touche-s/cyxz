package com.cyxz.circle.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 板块模板创建/更新请求
 * <p>模板是标准化定义，所有圈子共享同一套模板名称
 */
@Data
public class SectionTemplateRequest {

    @NotBlank(message = "板块名称不能为空")
    private String name;

    /** 适用内容类型：ALL=全部，NORMAL=图文帖，ARTICLE=长文。不填默认 ALL */
    private String applicableType;

    /** 模板描述，选填 */
    private String description;

    /** 排序值，数字越小越靠前。不填默认 0 */
    private Integer sortOrder;
}
