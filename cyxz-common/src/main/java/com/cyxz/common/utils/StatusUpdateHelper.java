package com.cyxz.common.utils;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 通用状态更新工具（乐观锁模式）
 */
public final class StatusUpdateHelper {

    private StatusUpdateHelper() {}

    /**
     * 条件更新记录状态
     * <p>仅当当前状态等于 expectedOldStatus 时才更新为目标状态，防止并发覆盖。
     *
     * @param mapper    MyBatis-Plus BaseMapper
     * @param id        记录主键
     * @param oldStatus 当前旧状态
     * @param newStatus 目标新状态
     * @param <T>       实体类型
     * @return true=更新成功（实际改变了一行）
     */
    public static <T> boolean updateStatus(BaseMapper<T> mapper, Long id, int oldStatus, int newStatus) {
        UpdateWrapper<T> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .eq("status", oldStatus)
                .set("status", newStatus);
        return mapper.update(null, wrapper) > 0;
    }
}
