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
 * 板块模板管理接口
 * <p>列表接口由 {@code circle:template:manage}（平台管理员管理模板）或
 * {@code circle:section:manage}（圈主/圈子管理员配置本圈板块）任一权限码即可读。
 * <p>增删改接口由 {@code circle:template:manage} 权限码控制（仅站主/平台管理员）。
 */
@RestController
@RequestMapping("/admin/section-template")
@RequiredArgsConstructor
public class SectionTemplateController {

    private final SectionTemplateService sectionTemplateService;

    /** 获取所有板块模板，平台管理员（管理模板）或圈主/圈子管理员（配置板块）可读 */
    @PreAuthorize("hasAuthority('circle:template:manage') or hasAuthority('circle:section:manage')")
    @GetMapping("/list")
    public Result<List<SectionTemplateVO>> list() {
        return Result.success(sectionTemplateService.listAll());
    }

    /** 创建板块模板（仅站主/平台管理员） */
    @PreAuthorize("hasAuthority('circle:template:manage')")
    @PostMapping
    public Result<SectionTemplateVO> create(@RequestBody SectionTemplateRequest dto) {
        return Result.success(sectionTemplateService.create(dto));
    }

    /** 更新板块模板（仅站主/平台管理员） */
    @PreAuthorize("hasAuthority('circle:template:manage')")
    @PutMapping("/{id}")
    public Result<SectionTemplateVO> update(@PathVariable Long id, @RequestBody SectionTemplateRequest dto) {
        return Result.success(sectionTemplateService.update(id, dto));
    }

    /** 删除板块模板（仅站主/平台管理员） */
    @PreAuthorize("hasAuthority('circle:template:manage')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sectionTemplateService.delete(id);
        return Result.success("删除成功");
    }
}
