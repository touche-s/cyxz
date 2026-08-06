package com.cyxz.auth.dto;

import lombok.Data;

/**
 * 用户-角色 code 映射（MyBatis 查询结果 DTO）
 */
@Data
public class UserRoleCode {

    private Long userId;

    private String roleCode;
}
