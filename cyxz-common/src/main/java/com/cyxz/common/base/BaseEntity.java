package com.cyxz.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PO 公共基类
 * <p>抽取所有实体表共有的时间审计字段，配合 {@link com.cyxz.common.config.MyMetaObjectHandler} 自动填充。
 * <p>继承本类后 PO 无需再重复声明 createTime/updateTime 及 serialVersionUID。
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
