package com.cyxz.common.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果
 * <p>统一的分页响应结构，包含当前页数据、总条数、当前页码、每页条数。
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页条数 */
    private int size;

    /**
     * 构建空结果
     *
     * @param page 页码
     * @param size 每页条数
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size);
    }

    /**
     * 构建结果
     *
     * @param records 数据列表
     * @param total   总记录数
     * @param page    页码
     * @param size    每页条数
     */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        return new PageResult<>(records, total, page, size);
    }
}
