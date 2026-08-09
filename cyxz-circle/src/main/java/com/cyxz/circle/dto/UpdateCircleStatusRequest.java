package com.cyxz.circle.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新圈子状态请求
 */
@Data
public class UpdateCircleStatusRequest {

    /** 状态：1=启用 0=禁用 */
    @NotNull(message = "status 不能为空")
    private Integer status;
}
