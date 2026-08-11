package com.cyxz.auth.vo;

import lombok.Data;

/**
 * 权限点 VO
 */
@Data
public class PermissionVO {

    private Long id;
    private String code;
    private String label;
    private String resource;
    private String action;
}
