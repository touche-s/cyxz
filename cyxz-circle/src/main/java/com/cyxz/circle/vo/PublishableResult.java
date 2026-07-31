package com.cyxz.circle.vo;

import lombok.Data;

/**
 * 圈子发布权限校验结果
 */
@Data
public class PublishableResult {

    private boolean exists;
    private boolean enabled;
    private boolean joined;
    private boolean publishable;
}
