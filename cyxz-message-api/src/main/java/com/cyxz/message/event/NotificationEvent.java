package com.cyxz.message.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long receiverId;

    private Long senderId;

    private String type;

    private String title;

    private String content;

    private String targetType;

    private Long targetId;

    private Long relatedId;

    private Long createTime;
}
