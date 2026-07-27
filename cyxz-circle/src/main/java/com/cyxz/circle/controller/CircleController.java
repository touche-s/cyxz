package com.cyxz.circle.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.circle.service.CircleService;
import com.cyxz.circle.vo.CircleVO;
import com.cyxz.circle.vo.PublishableResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/circle")
@RequiredArgsConstructor
public class CircleController {

    private final CircleService circleService;

    @GetMapping("/list")
    public Result<List<CircleVO>> list(@CurrentUser(required = false) Long currentUserId) {
        return Result.success(circleService.listAll(currentUserId));
    }

    @GetMapping("/{circleId}")
    public Result<CircleVO> detail(@PathVariable Long circleId,
                                   @CurrentUser(required = false) Long currentUserId) {
        return Result.success(circleService.getById(circleId, currentUserId));
    }

    @GetMapping("/hot")
    public Result<PageResult<CircleVO>> hot(@RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                            @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size,
                                            @CurrentUser(required = false) Long currentUserId) {
        return Result.success(circleService.listHot(page, size, currentUserId));
    }

    @GetMapping("/joined")
    public Result<List<CircleVO>> joined(@CurrentUser Long userId) {
        return Result.success(circleService.listJoined(userId));
    }

    @PostMapping("/{circleId}/join")
    public Result<Void> join(@PathVariable Long circleId, @CurrentUser Long userId) {
        circleService.joinCircle(userId, circleId);
        return Result.success();
    }

    @DeleteMapping("/{circleId}/join")
    public Result<Void> leave(@PathVariable Long circleId, @CurrentUser Long userId) {
        circleService.leaveCircle(userId, circleId);
        return Result.success();
    }

    @GetMapping("/internal/{circleId}/publishable")
    public Result<PublishableResult> checkPublishable(@PathVariable Long circleId,
                                                       @RequestParam Long userId) {
        return Result.success(circleService.checkPublishable(circleId, userId));
    }

    @GetMapping("/internal/batch-names")
    public Result<Map<Long, String>> batchNames(@RequestParam Set<Long> circleIds) {
        return Result.success(circleService.batchGetNames(circleIds));
    }
}
