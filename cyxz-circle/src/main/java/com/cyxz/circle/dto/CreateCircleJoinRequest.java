package com.cyxz.circle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交入圈申请
 */
@Data
public class CreateCircleJoinRequest {

    /** 要加入的圈子 ID（必填） */
    @NotNull(message = "圈子ID不能为空")
    private Long circleId;

    /** 申请理由（可选，最长 200 字） */
    @Size(max = 200, message = "申请理由不能超过200字")
    private String reason;
}
