package com.cyxz.governance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 举报处理意见请求（通过/驳回时必填）
 */
@Data
public class HandlerNoteRequest {

    /** 处理意见 */
    @NotBlank(message = "处理意见不能为空")
    @Size(max = 500, message = "处理意见不能超过500字")
    private String note;
}
