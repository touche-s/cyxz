package com.cyxz.post.controller;

import com.cyxz.common.base.Result;
import com.cyxz.post.service.CategoryService;
import com.cyxz.post.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 查询所有启用的分类
     *
     * @return 分类列表（按 sortOrder 升序）
     */
    @GetMapping("/list")
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.listAll());
    }
}
