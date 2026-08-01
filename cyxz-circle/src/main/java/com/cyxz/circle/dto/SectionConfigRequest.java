package com.cyxz.circle.dto;

import lombok.Data;

/**
 * 圈子板块配置请求，管理员为圈子配置板块时使用
 * <p>采用全量替换策略：提交的 configs 列表即最终状态，先删后插
 */
@Data
public class SectionConfigRequest {

    /** 板块模板 ID */
    private Long templateId;
    /** 是否默认板块：1=是，0=否。发帖时默认选中该板块 */
    private Integer isDefault;
    /** 排序值，数字越小越靠前，通常按数组索引赋值 */
    private Integer sortOrder;
    /** 板块状态：1=启用，0=禁用。使用 CommonStatus 枚举值 */
    private Integer status;
}
