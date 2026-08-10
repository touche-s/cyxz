package com.cyxz.circle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交圈子创建申请
 */
@Data
public class CreateCircleApplicationRequest {

    /** 圈子名称（必填，最长 30 字） */
    @NotBlank(message = "圈子名称不能为空")
    @Size(max = 30, message = "圈子名称最长30字")
    private String name;

    /** 圈子简介（可选，最长 100 字） */
    @Size(max = 100, message = "圈子简介最长100字")
    private String intro;

    /** 圈子头像 URL（可选） */
    private String avatar;

    /** 圈子封面 URL（可选） */
    private String cover;
}
