package com.cyxz.circle.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.circle.dto.HandlerNoteRequest;
import com.cyxz.circle.service.CircleApplicationService;
import com.cyxz.circle.vo.CircleApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 建圈申请管理控制器（平台管理员）
 * <p>审核通过后直接调用 {@link com.cyxz.circle.service.CircleService#createCircle} 建圈，
 * 同模块内同步事务完成，不再发布 MQ 事件。
 */
@Tag(name = "建圈申请管理（平台管理员）", description = "建圈申请审核")
@RestController
@RequestMapping("/admin/circle-application")
@RequiredArgsConstructor
public class CircleApplicationAdminController {

    private final CircleApplicationService applicationService;

    /**
     * 建圈申请列表（按状态筛选，分页）
     *
     * @param status 状态筛选（null=全部：PENDING / APPROVED / REJECTED）
     * @param page   页码
     * @param size   每页条数
     */
    @Operation(summary = "建圈申请列表")
    @PreAuthorize("hasAuthority('circle:application:review:list')")
    @GetMapping("/list")
    public Result<PageResult<CircleApplicationVO>> list(@RequestParam(value = "status", required = false) String status,
                                                        @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                        @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size) {
        return Result.success(applicationService.listForAdmin(status, page, size));
    }

    /**
     * 建圈申请详情
     *
     * @param id 申请记录 ID
     */
    @Operation(summary = "建圈申请详情")
    @PreAuthorize("hasAuthority('circle:application:review:list')")
    @GetMapping("/{id}")
    public Result<CircleApplicationVO> detail(@PathVariable Long id) {
        return Result.success(applicationService.getDetail(id));
    }

    /**
     * 通过建圈申请，直接建圈
     *
     * @param id      申请记录 ID
     * @param userId  当前管理员用户 ID
     * @param request 审核意见（必填）
     */
    @Operation(summary = "通过建圈申请")
    @PreAuthorize("hasAuthority('circle:application:review:approve')")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @CurrentUser Long userId,
                                @Valid @RequestBody HandlerNoteRequest request) {
        applicationService.approveApplication(id, userId, request.getNote());
        return Result.success("已通过建圈申请");
    }

    /**
     * 驳回建圈申请
     *
     * @param id      申请记录 ID
     * @param userId  当前管理员用户 ID
     * @param request 审核意见（必填）
     */
    @Operation(summary = "驳回建圈申请")
    @PreAuthorize("hasAuthority('circle:application:review:reject')")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @CurrentUser Long userId,
                               @Valid @RequestBody HandlerNoteRequest request) {
        applicationService.rejectApplication(id, userId, request.getNote());
        return Result.success("已驳回建圈申请");
    }
}
