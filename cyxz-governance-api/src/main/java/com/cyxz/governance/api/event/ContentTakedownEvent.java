package com.cyxz.governance.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 内容处置事件
 * <p>治理中心审核通过举报后发布，由 post / comment 服务消费以隐藏或删除对应内容，
 * 实现跨服务内容处置的最终一致性，避免 governance 直接写业务库。
 *
 * <pre>{@code
 * governance 审核通过举报
 *      ↓ 发布 ContentTakedownEvent
 * post / comment 消费 → 删除/隐藏内容
 * message 消费 → 通知举报人与内容作者
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTakedownEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 处置目标类型：POST / COMMENT */
    private String targetType;

    /** 处置目标 ID */
    private Long targetId;

    /** 关联举报单 ID */
    private Long reportId;

    /** 处理人（管理员）用户 ID */
    private Long operatorId;
}
