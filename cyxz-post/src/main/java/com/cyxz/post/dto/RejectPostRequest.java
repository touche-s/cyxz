package com.cyxz.post.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审核拒绝请求
 */
@Data
public class RejectPostRequest {

    /** 拒绝原因 */
    @NotBlank(message = "拒绝原因不能为空")
    private String reason;
}
