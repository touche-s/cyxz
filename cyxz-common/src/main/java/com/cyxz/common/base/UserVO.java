package com.cyxz.common.base;

import lombok.Data;

/**
 * 用户基础信息 VO
 * <p>用于服务间传输用户公开信息，供 Feign 调用和业务组装使用。
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String signature;
}
