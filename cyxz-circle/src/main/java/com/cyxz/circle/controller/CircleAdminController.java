package com.cyxz.circle.controller;

import com.cyxz.common.base.Result;
import com.cyxz.common.web.CurrentUser;
import com.cyxz.circle.dto.UpdateCircleStatusRequest;
import com.cyxz.circle.service.CircleService;
import com.cyxz.circle.vo.CircleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 圈子管理端 Controller，平台管理员操作
 */
@Tag(name = "圈子管理", description = "平台管理端圈子接口")
@RestController
@RequestMapping("/admin/circle")
@RequiredArgsConstructor
@Validated
public class CircleAdminController {

    private final CircleService circleService;

    @Operation(summary = "管理员全量圈子列表（含禁用状态）")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('circle:admin:list')")
    public Result<List<CircleVO>> adminList() {
        return Result.success(circleService.listAllForAdmin());
    }

    @Operation(summary = "创建圈子，创建者成为圈主")
    @PostMapping
    @PreAuthorize("hasAuthority('circle:manage:create')")
    public Result<CircleVO> create(@RequestParam @NotBlank(message = "圈子名称不能为空") @Size(max = 30, message = "圈子名称最长30字") String name,
                                   @RequestParam(required = false) @Size(max = 100, message = "圈子简介最长100字") String intro,
                                   @RequestParam(required = false) String avatar,
                                   @RequestParam(required = false) String cover,
                                   @CurrentUser Long ownerId) {
        return Result.success(circleService.createCircle(name, intro, avatar, cover, ownerId));
    }

    @Operation(summary = "删除圈子")
    @DeleteMapping("/{circleId}")
    @PreAuthorize("hasAuthority('circle:manage:delete')")
    public Result<Void> delete(@PathVariable Long circleId) {
        circleService.deleteCircle(circleId);
        return Result.success("删除成功");
    }

    @Operation(summary = "更新圈子状态（启用/禁用）")
    @PutMapping("/{circleId}/status")
    @PreAuthorize("hasAuthority('circle:status:update')")
    public Result<Void> updateStatus(@PathVariable Long circleId, @Valid @RequestBody UpdateCircleStatusRequest request) {
        circleService.updateStatus(circleId, request.getStatus());
        return Result.success("状态更新成功");
    }
}
