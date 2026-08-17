package com.cyxz.audit.controller;

import com.cyxz.audit.service.AuditService;
import com.cyxz.audit.vo.AuditLogVO;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 审计日志管理控制器（平台管理员）
 * <p>提供审计日志的多条件分页查询，供平台管理员追溯关键操作行为。
 */
@Tag(name = "审计日志管理（平台管理员）")
@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AuditAdminController {

    private final AuditService auditService;

    /**
     * 审计日志列表（按动作/对象类型/操作人/时间范围筛选，分页）
     *
     * @param userId     当前登录用户 ID（确保已登录）
     * @param action     动作筛选（null=全部）
     * @param targetType 对象类型筛选（null=全部）
     * @param operatorId 操作人用户 ID 筛选（null=全部）
     * @param startDate  起始日期（含，可选，格式 yyyy-MM-dd）
     * @param endDate    结束日期（含，可选，格式 yyyy-MM-dd）
     * @param page       页码
     * @param size       每页条数
     * @return 审计日志分页列表
     */
    @Operation(summary = "审计日志列表")
    @PreAuthorize("hasAuthority('audit:log:list')")
    @GetMapping("/list")
    public Result<PageResult<AuditLogVO>> list(@CurrentUser Long userId,
                                                @RequestParam(value = "action", required = false) String action,
                                                @RequestParam(value = "targetType", required = false) String targetType,
                                                @RequestParam(value = "operatorId", required = false) Long operatorId,
                                                @RequestParam(value = "startDate", required = false)
                                                @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                @RequestParam(value = "endDate", required = false)
                                                @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                                @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size) {
        return Result.success(auditService.list(action, targetType, operatorId, startDate, endDate, page, size));
    }
}
