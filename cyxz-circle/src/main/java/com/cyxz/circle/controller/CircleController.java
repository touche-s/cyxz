package com.cyxz.circle.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.AdminUser;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.circle.dto.SectionConfigRequest;
import com.cyxz.circle.service.CircleSectionService;
import com.cyxz.circle.service.CircleService;
import com.cyxz.circle.vo.CircleSectionVO;
import com.cyxz.circle.vo.CircleVO;
import com.cyxz.circle.vo.PublishableResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 圈子 Controller，提供圈子的公共查询和内部服务接口
 */
@RestController
@RequestMapping("/circle")
@RequiredArgsConstructor
public class CircleController {

    private final CircleService circleService;
    private final CircleSectionService circleSectionService;

    /**
     * 全量启用圈子列表
     */
    @GetMapping("/list")
    public Result<List<CircleVO>> list(@CurrentUser(required = false) Long currentUserId) {
        return Result.success(circleService.listAll(currentUserId));
    }

    /**
     * 圈子详情
     */
    @GetMapping("/{circleId}")
    public Result<CircleVO> detail(@PathVariable Long circleId,
                                   @CurrentUser(required = false) Long currentUserId) {
        return Result.success(circleService.getById(circleId, currentUserId));
    }

    /**
     * 热门圈子分页（按成员数降序）
     * @param page 页码
     * @param size 每页数量
     * @param currentUserId 当前登录用户 ID，未登录时为 null
     * @return 热门圈子分页结果
     */
    @GetMapping("/hot")
    public Result<PageResult<CircleVO>> hot(@RequestParam(value = "page", defaultValue = PageConstants.DEFAULT_PAGE_STR) int page,
                                            @RequestParam(value = "size", defaultValue = PageConstants.DEFAULT_SIZE_STR) int size,
                                            @CurrentUser(required = false) Long currentUserId) {
        return Result.success(circleService.listHot(page, size, currentUserId));
    }

    /**
     * 当前用户已加入的圈子
     */
    @GetMapping("/joined")
    public Result<List<CircleVO>> joined(@CurrentUser Long userId) {
        return Result.success(circleService.listJoined(userId));
    }

    /**
     * 加入圈子
     */
    @PostMapping("/{circleId}/join")
    public Result<Void> join(@PathVariable Long circleId, @CurrentUser Long userId) {
        circleService.joinCircle(userId, circleId);
        return Result.success();
    }

    /**
     * 退出圈子
     */
    @DeleteMapping("/{circleId}/join")
    public Result<Void> leave(@PathVariable Long circleId, @CurrentUser Long userId) {
        circleService.leaveCircle(userId, circleId);
        return Result.success();
    }

    /**
     * 更新圈子资料
     */
    @PutMapping("/{circleId}")
    public Result<Void> update(@PathVariable Long circleId,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) String intro,
                               @RequestParam(required = false) String avatar,
                               @RequestParam(required = false) String cover,
                               @AdminUser Object admin) {
        circleService.updateCircle(circleId, name, intro, avatar, cover);
        return Result.success("更新成功");
    }

    /**
     * 创建圈子
     */
    @PostMapping
    public Result<CircleVO> create(@RequestParam String name,
                                   @RequestParam(required = false) String intro,
                                   @RequestParam(required = false) String avatar,
                                   @RequestParam(required = false) String cover,
                                   @AdminUser Object admin) {
        return Result.success(circleService.createCircle(name, intro, avatar, cover));
    }

    /**
     * 删除圈子
     */
    @DeleteMapping("/{circleId}")
    public Result<Void> delete(@PathVariable Long circleId, @AdminUser Object admin) {
        circleService.deleteCircle(circleId);
        return Result.success("删除成功");
    }

    /**
     * 内部接口：校验发布权限
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @return 发布权限校验结果
     */
    @GetMapping("/internal/{circleId}/publishable")
    public Result<PublishableResult> checkPublishable(@PathVariable Long circleId,
                                                       @RequestParam Long userId) {
        return Result.success(circleService.checkPublishable(circleId, userId));
    }

    /**
     * 内部接口：批量查询圈子名称
     */
    @GetMapping("/internal/batch-names")
    public Result<Map<Long, String>> batchNames(@RequestParam Set<Long> circleIds) {
        return Result.success(circleService.batchGetNames(circleIds));
    }

    /**
     * 查询圈子板块列表
     */
    @GetMapping("/{circleId}/sections")
    public Result<List<CircleSectionVO>> sections(@PathVariable Long circleId) {
        return Result.success(circleSectionService.listByCircleId(circleId));
    }

    /**
     * 管理员配置圈子板块
     */
    @PutMapping("/{circleId}/sections")
    public Result<Void> configureSections(@PathVariable Long circleId,
                                          @RequestBody List<SectionConfigRequest> configs,
                                          @AdminUser Object admin) {
        circleSectionService.configureSections(circleId, configs);
        return Result.success("配置成功");
    }

    /**
     * 内部接口：校验板块是否属于指定圈子
     */
    @GetMapping("/internal/section/validate")
    public Result<Boolean> validateSection(@RequestParam Long sectionId,
                                           @RequestParam Long circleId) {
        return Result.success(circleSectionService.validateSection(sectionId, circleId));
    }

    /**
     * 内部接口：批量查询板块名称
     * @param sectionIds 板块 ID 集合
     * @return 板块 ID 到名称的映射
     */
    @GetMapping("/internal/section/batch-names")
    public Result<Map<Long, String>> batchSectionNames(@RequestParam Set<Long> sectionIds) {
        return Result.success(circleSectionService.batchGetSectionNames(sectionIds));
    }
}
