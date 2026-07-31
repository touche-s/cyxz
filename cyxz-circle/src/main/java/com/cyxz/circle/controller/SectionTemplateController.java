package com.cyxz.circle.controller;

import com.cyxz.common.base.Result;
import com.cyxz.common.web.AdminUser;
import com.cyxz.circle.dto.SectionTemplateDTO;
import com.cyxz.circle.service.SectionTemplateService;
import com.cyxz.circle.vo.SectionTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 板块模板管理接口（管理员专属）
 * <p>admin/section-template 路径下的接口前端通过请求拦截器校验管理员权限
 */
@RestController
@RequestMapping("/admin/section-template")
@RequiredArgsConstructor
public class SectionTemplateController {

    private final SectionTemplateService sectionTemplateService;

    /** 获取所有板块模板，管理后台展示用 */
    @GetMapping("/list")
    public Result<List<SectionTemplateVO>> list(@AdminUser Object admin) {
        return Result.success(sectionTemplateService.listAll());
    }

    /** 创建板块模板 */
    @PostMapping
    public Result<SectionTemplateVO> create(@RequestBody SectionTemplateDTO dto, @AdminUser Object admin) {
        return Result.success(sectionTemplateService.create(dto));
    }

    /** 更新板块模板 */
    @PutMapping("/{id}")
    public Result<SectionTemplateVO> update(@PathVariable Long id, @RequestBody SectionTemplateDTO dto, @AdminUser Object admin) {
        return Result.success(sectionTemplateService.update(id, dto));
    }

    /** 删除板块模板 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @AdminUser Object admin) {
        sectionTemplateService.delete(id);
        return Result.success("删除成功");
    }
}
