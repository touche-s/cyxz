package com.cyxz.circle.controller;

import com.cyxz.common.base.Result;
import com.cyxz.circle.dto.SectionTemplateRequest;
import com.cyxz.circle.service.SectionTemplateService;
import com.cyxz.circle.vo.SectionTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 板块模板管理接口（平台管理员专属）
 * <p>授权由 {@code @PreAuthorize} 方法注解控制，仅站主/平台管理员可访问。
 */
@RestController
@RequestMapping("/admin/section-template")
@RequiredArgsConstructor
public class SectionTemplateController {

    private final SectionTemplateService sectionTemplateService;

    /** 获取所有板块模板，管理后台展示用 */
    @PreAuthorize("hasRole('SITE_OWNER') or hasRole('PLATFORM_ADMIN')")
    @GetMapping("/list")
    public Result<List<SectionTemplateVO>> list() {
        return Result.success(sectionTemplateService.listAll());
    }

    /** 创建板块模板 */
    @PreAuthorize("hasRole('SITE_OWNER') or hasRole('PLATFORM_ADMIN')")
    @PostMapping
    public Result<SectionTemplateVO> create(@RequestBody SectionTemplateRequest dto) {
        return Result.success(sectionTemplateService.create(dto));
    }

    /**
     * 更新板块模板
     *
     * @param id  板块模板 ID
     * @param dto 板块模板更新请求
     * @return 更新后的板块模板信息
     */
    @PreAuthorize("hasRole('SITE_OWNER') or hasRole('PLATFORM_ADMIN')")
    @PutMapping("/{id}")
    public Result<SectionTemplateVO> update(@PathVariable Long id, @RequestBody SectionTemplateRequest dto) {
        return Result.success(sectionTemplateService.update(id, dto));
    }

    /** 删除板块模板 */
    @PreAuthorize("hasRole('SITE_OWNER') or hasRole('PLATFORM_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sectionTemplateService.delete(id);
        return Result.success("删除成功");
    }
}
