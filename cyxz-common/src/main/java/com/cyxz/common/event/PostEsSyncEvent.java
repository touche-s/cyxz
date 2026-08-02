package com.cyxz.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 帖子 ES 索引同步事件，发到 RabbitMQ 由 cyxz-search 消费
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostEsSyncEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** CREATE / UPDATE / DELETE */
    private String action;

    private Long postId;
    private Long userId;
    private Long circleId;
    private Long sectionId;
    private String postType;
    private String title;
    private String content;
    private String cover;
    private String tags;
    private Integer status;
    private Integer likes;
    private Integer comments;
    private Integer views;
    private Integer collections;

    /** 创建时间 epoch 毫秒 */
    private Long createTime;
}
