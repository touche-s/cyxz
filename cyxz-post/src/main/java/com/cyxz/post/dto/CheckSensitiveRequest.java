package com.cyxz.post.dto;

import lombok.Data;

/**
 * 敏感词检测请求
 */
@Data
public class CheckSensitiveRequest {

    /** 标题（可为空） */
    private String title = "";

    /** 正文（可为空） */
    private String content = "";
}
