package com.cyxz.post.service;

import com.cyxz.post.vo.CategoryVO;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {

    /**
     * 查询所有启用的分类
     * <p>仅返回 status=1 的分类，按 sortOrder 升序排列。
     *
     * @return 分类视图列表
     */
    List<CategoryVO> listAll();
}
