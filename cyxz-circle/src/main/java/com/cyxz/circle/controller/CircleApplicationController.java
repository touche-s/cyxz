package com.cyxz.circle.controller;

import com.cyxz.common.annotation.PreventRepeat;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.circle.dto.CreateCircleApplicationRequest;
import com.cyxz.circle.service.CircleApplicationService;
import com.cyxz.circle.vo.CircleApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 圈子创建申请控制器（用户端）
 */
@Tag(name = "建圈申请（用户端）", description = "提交建圈申请、查看我的申请")
@RestController
@RequestMapping("/circle-application")
@RequiredArgsConstructor
public class CircleApplicationController {

    private final CircleApplicationService applicationService;

    /**
     * 提交建圈申请
     * <p>同一用户有待审核申请时不允许重复提交。
     *
     * @param request 申请请求（name / intro / avatar / cover）
     * @param userId  当前登录用户 ID
     * @return 申请记录 ID
     */
    @Operation(summary = "提交建圈申请")
    @PreventRepeat(interval = 5)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateCircleApplicationRequest request,
                               @CurrentUser Long userId) {
        return Result.success("申请已提交", applicationService.createApplication(userId, request));
    }

    /**
     * 查询我的建圈申请
     *
     * @param userId 当前登录用户 ID
     * @param page   页码
     * @param size   每页条数
     */
    @Operation(summary = "我的建圈申请")
    @GetMapping("/mine")
    public Result<PageResult<CircleApplicationVO>> mine(@CurrentUser Long userId,
                                                        @RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                                        @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size) {
        return Result.success(applicationService.listByApplicant(userId, page, size));
    }
}
