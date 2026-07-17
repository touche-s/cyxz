package com.cyxz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.post.entity.CategoryPO;
import com.cyxz.post.mapper.CategoryMapper;
import com.cyxz.post.service.CategoryService;
import com.cyxz.post.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分类服务实现
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    /**
     * 查询所有启用的分类
     * <p>仅返回 status=1 的分类，按 sortOrder 升序排列。
     *
     * @return 分类视图列表
     */
    @Override
    public List<CategoryVO> listAll() {
        LambdaQueryWrapper<CategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategoryPO::getStatus, 1);
        wrapper.orderByAsc(CategoryPO::getSortOrder);
        List<CategoryPO> categories = categoryMapper.selectList(wrapper);
        return categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 批量查询分类（id → CategoryPO）
     *
     * @param ids 分类 ID 集合
     * @return Map<分类ID, 分类实体>
     */
    @Override
    public Map<Long, CategoryPO> getByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CategoryPO> categories = categoryMapper.selectBatchIds(ids);
        return categories.stream()
                .collect(Collectors.toMap(CategoryPO::getId, Function.identity()));
    }

    /**
     * 将分类实体转换为视图对象
     *
     * @param po 分类实体
     * @return 分类视图对象
     */
    private CategoryVO convertToVO(CategoryPO po) {
        CategoryVO vo = new CategoryVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setDescription(po.getDescription());
        vo.setSortOrder(po.getSortOrder());
        return vo;
    }
}
