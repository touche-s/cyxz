package com.cyxz.auth.vo;

import lombok.Data;

/**
 * 角色管理 VO
 */
@Data
public class RoleVO {

    private Long id;
    private String code;
    private String label;
    private String scope;
    private String description;
    private Integer builtIn;
    private Integer sort;
}
