package com.cyxz.circle.controller;

import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.circle.dto.SectionConfigRequest;
import com.cyxz.circle.dto.UpdateCircleStatusRequest;
import com.cyxz.circle.service.CircleSectionService;
import com.cyxz.circle.service.CircleService;
import com.cyxz.circle.vo.CircleSectionVO;
import com.cyxz.circle.vo.CircleVO;
import com.cyxz.circle.vo.MemberVO;
import com.cyxz.circle.vo.PublishableResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 圈子 Controller，提供圈子的公共查询和内部服务接口
 * <p>写操作权限校验：平台级操作用全局权限码（circle:manage:create/delete），
 * 圈子内操作用 {@code @circlePerm} 校验圈子权限（circle:manage:update/section:manage），
 * 全局管理员（站主/平台管理员）对所有圈子均放行。
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
     * 管理员全量圈子列表（含禁用状态），用于平台管理后台
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('circle:admin:list')")
    public Result<List<CircleVO>> adminList() {
        return Result.success(circleService.listAllForAdmin());
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
     * 当前用户管理的圈子（圈主或圈子管理员）
     * <p>用于圈子管理后台左侧圈子选择器。
     */
    @GetMapping("/managed")
    public Result<List<CircleVO>> managed(@CurrentUser Long userId) {
        return Result.success(circleService.listManagedCircles(userId));
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
     * <p>全局管理员（站主/平台管理员）可改任意圈子；圈主/管理员可改自己圈子（circle:manage:update）。
     * <p>全局管理员短路由 {@link com.cyxz.common.security.CirclePermissionEvaluator} 内部处理。
     */
    @PutMapping("/{circleId}")
    @PreAuthorize("@circlePerm.hasAuthority('circle:manage:update', #circleId)")
    public Result<Void> update(@PathVariable Long circleId,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) String intro,
                               @RequestParam(required = false) String avatar,
                               @RequestParam(required = false) String cover) {
        circleService.updateCircle(circleId, name, intro, avatar, cover);
        return Result.success("更新成功");
    }

    /**
     * 创建圈子，创建者成为圈主
     */
    @PostMapping
    @PreAuthorize("hasAuthority('circle:manage:create')")
    public Result<CircleVO> create(@RequestParam String name,
                                   @RequestParam(required = false) String intro,
                                   @RequestParam(required = false) String avatar,
                                   @RequestParam(required = false) String cover,
                                   @CurrentUser Long ownerId) {
        return Result.success(circleService.createCircle(name, intro, avatar, cover, ownerId));
    }

    /**
     * 删除圈子（仅平台管理员）
     */
    @DeleteMapping("/{circleId}")
    @PreAuthorize("hasAuthority('circle:manage:delete')")
    public Result<Void> delete(@PathVariable Long circleId) {
        circleService.deleteCircle(circleId);
        return Result.success("删除成功");
    }

    /**
     * 更新圈子状态（启用/禁用，仅平台管理员）
     *
     * @param circleId 圈子 ID
     * @param body     请求体，包含 status（1=启用 0=禁用）
     */
    @PutMapping("/{circleId}/status")
    @PreAuthorize("hasAuthority('circle:status:update')")
    public Result<Void> updateStatus(@PathVariable Long circleId, @Valid @RequestBody UpdateCircleStatusRequest request) {
        circleService.updateStatus(circleId, request.getStatus());
        return Result.success("状态更新成功");
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
     * 配置圈子板块（全局管理员或圈子管理员/圈主）
     * <p>全局管理员短路由 {@link com.cyxz.common.security.CirclePermissionEvaluator} 内部处理。
     */
    @PutMapping("/{circleId}/sections")
    @PreAuthorize("@circlePerm.hasAuthority('circle:section:manage', #circleId)")
    public Result<Void> configureSections(@PathVariable Long circleId,
                                          @RequestBody List<SectionConfigRequest> configs) {
        circleSectionService.configureSections(circleId, configs);
        return Result.success("配置成功");
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

    // ===== 圈子成员管理（仅圈主 circle:member:manage） =====

    /**
     * 查询圈子成员列表（含角色信息），按圈主→管理员→成员排序
     */
    @GetMapping("/{circleId}/members")
    public Result<List<MemberVO>> listMembers(@PathVariable Long circleId) {
        return Result.success(circleService.listMembers(circleId));
    }

    /**
     * 任命圈子管理员
     * <p>仅圈主可操作（{@code circle:member:manage}），目标用户必须是圈子成员。
     *
     * @param circleId 圈子 ID
     * @param userId   目标用户 ID
     */
    @PreAuthorize("@circlePerm.hasAuthority('circle:member:manage', #circleId)")
    @PutMapping("/{circleId}/members/{userId}/promote")
    public Result<Void> appointAdmin(@PathVariable Long circleId, @PathVariable Long userId) {
        circleService.appointAdmin(circleId, userId);
        return Result.success("已任命为圈子管理员");
    }

    /**
     * 撤销圈子管理员，降级为普通成员
     * <p>仅圈主可操作（{@code circle:member:manage}）。
     *
     * @param circleId 圈子 ID
     * @param userId   目标用户 ID
     */
    @PreAuthorize("@circlePerm.hasAuthority('circle:member:manage', #circleId)")
    @PutMapping("/{circleId}/members/{userId}/demote")
    public Result<Void> removeAdmin(@PathVariable Long circleId, @PathVariable Long userId) {
        circleService.removeAdmin(circleId, userId);
        return Result.success("已撤销管理员");
    }

    /**
     * 移除圈子成员（踢出），撤销该用户在该圈子中的所有角色并更新成员数
     * <p>仅圈主可操作（{@code circle:member:manage}），不可踢出圈主本人。
     *
     * @param circleId 圈子 ID
     * @param userId   目标用户 ID
     */
    @PreAuthorize("@circlePerm.hasAuthority('circle:member:manage', #circleId)")
    @DeleteMapping("/{circleId}/members/{userId}")
    public Result<Void> kickMember(@PathVariable Long circleId, @PathVariable Long userId) {
        circleService.kickMember(circleId, userId);
        return Result.success("已移除成员");
    }
}
