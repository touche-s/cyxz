package com.cyxz.governance.controller;

import com.cyxz.common.annotation.PreventRepeat;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.governance.dto.CreateReportRequest;
import com.cyxz.governance.service.ReportService;
import com.cyxz.governance.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报控制器（用户端）
 */
@Tag(name = "举报（用户端）", description = "提交举报")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 提交举报
     * <p>同一用户对同一对象仅可举报一次（uk_reporter_target 约束）。
     *
     * @param request 举报请求（targetType / targetId / reason）
     * @param userId  当前登录用户 ID
     * @return 举报记录 ID
     */
    @Operation(summary = "提交举报")
    @PreventRepeat(interval = 5)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateReportRequest request,
                               @CurrentUser Long userId) {
        return Result.success("举报已提交", reportService.createReport(userId, request));
    }

    @Operation(summary = "我的举报记录")
    @GetMapping("/mine")
    public Result<PageResult<ReportVO>> mine(@RequestParam(value = "status", required = false) String status,
                                              @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                              @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size,
                                              @CurrentUser Long userId) {
        return Result.success(reportService.listMine(userId, status, page, size));
    }
}
