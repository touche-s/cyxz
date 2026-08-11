package com.cyxz.post.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量操作帖子请求
 */
@Data
public class BatchOperateRequest {

    /** 帖子 ID 列表 */
    @NotEmpty(message = "postIds 不能为空")
    @NotNull(message = "postIds 不能为空")
    private List<Long> postIds;

    /** 操作类型：publish / delete 等 */
    @NotEmpty(message = "action 不能为空")
    private String action;
}
