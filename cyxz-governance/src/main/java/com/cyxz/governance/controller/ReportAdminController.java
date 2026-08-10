package com.cyxz.governance.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.governance.dto.HandlerNoteRequest;
import com.cyxz.governance.service.ReportService;
import com.cyxz.governance.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 举报管理控制器（平台管理员）
 * <p>承载举报列表、详情、通过、驳回。审核通过后由 service 发布内容处置事件，
 * 由 post / comment 服务消费删除内容，由 message 服务发送通知。
 */
@Tag(name = "举报管理（平台管理员）", description = "举报处理台")
@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportAdminController {

    private final ReportService reportService;

    /**
     * 举报列表（按状态/类型筛选，分页）
     *
     * @param status     状态筛选（null=全部：PENDING / APPROVED / REJECTED）
     * @param targetType 类型筛选（null=全部：POST / COMMENT）
     * @param page       页码
     * @param size       每页条数
     */
    @Operation(summary = "举报列表")
    @PreAuthorize("hasAuthority('report:review:list')")
    @GetMapping("/list")
    public Result<PageResult<ReportVO>> list(@RequestParam(value = "status", required = false) String status,
                                              @RequestParam(value = "targetType", required = false) String targetType,
                                              @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                              @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size) {
        return Result.success(reportService.listForAdmin(status, targetType, page, size));
    }

    /**
     * 举报详情
     *
     * @param id 举报记录 ID
     */
    @Operation(summary = "举报详情")
    @PreAuthorize("hasAuthority('report:review:list')")
    @GetMapping("/{id}")
    public Result<ReportVO> detail(@PathVariable Long id) {
        return Result.success(reportService.getReportDetail(id));
    }

    /**
     * 通过举报（发布内容处置事件，删除/隐藏对应内容）
     *
     * @param id      举报记录 ID
     * @param userId  当前管理员用户 ID
     * @param request 处理意见（必填）
     */
    @Operation(summary = "通过举报")
    @PreAuthorize("hasAuthority('report:review:approve')")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @CurrentUser Long userId,
                                @Valid @RequestBody HandlerNoteRequest request) {
        reportService.approveReport(id, userId, request.getNote());
        return Result.success("已通过举报");
    }

    /**
     * 驳回举报
     *
     * @param id      举报记录 ID
     * @param userId  当前管理员用户 ID
     * @param request 处理意见（必填）
     */
    @Operation(summary = "驳回举报")
    @PreAuthorize("hasAuthority('report:review:reject')")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @CurrentUser Long userId,
                               @Valid @RequestBody HandlerNoteRequest request) {
        reportService.rejectReport(id, userId, request.getNote());
        return Result.success("已驳回举报");
    }
}
