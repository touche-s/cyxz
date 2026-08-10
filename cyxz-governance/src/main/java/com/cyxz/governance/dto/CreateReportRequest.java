package com.cyxz.governance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交举报请求
 */
@Data
public class CreateReportRequest {

    /** 举报对象类型：POST / COMMENT */
    @NotBlank(message = "举报对象类型不能为空")
    private String targetType;

    /** 举报对象 ID */
    @NotNull(message = "举报对象ID不能为空")
    private Long targetId;

    /** 举报原因 */
    @NotBlank(message = "举报原因不能为空")
    @Size(max = 200, message = "举报原因不能超过200字")
    private String reason;
}
