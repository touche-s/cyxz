package com.cyxz.message.utils;

import com.cyxz.message.constant.NotificationConstants;
import com.cyxz.message.enums.NotificationTargetType;
import com.cyxz.message.enums.NotificationType;
import com.cyxz.message.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 通知事件发布工具
 * <p>封装 RabbitMQ 通知发布的 try/catch 模板，失败仅记录日志不抛异常，避免阻塞业务主流程。
 * <p>调用方注入 {@link RabbitTemplate} 后调用 {@link #publish}；构建事件优先用 {@link #of} 系列工厂方法，
 * 已预设 createTime，避免散落各处的 {@code System.currentTimeMillis()} 重复。
 */
@Slf4j
public final class NotificationPublisher {

    private NotificationPublisher() {}

    /**
     * 构建基础通知事件（预设 createTime）
     * <p>适用于点赞/关注等无 content/relatedId 的通知场景。
     *
     * @param receiverId 接收者 ID
     * @param senderId   发送者 ID
     * @param type       通知类型
     * @param title      通知标题
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 已预设 createTime 的 NotificationEvent
     */
    public static NotificationEvent of(Long receiverId, Long senderId, NotificationType type,
                                       String title, String targetType, Long targetId) {
        return NotificationEvent.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .type(type.name())
                .title(title)
                .targetType(targetType)
                .targetId(targetId)
                .createTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 构建带正文与关联 ID 的通知事件（预设 createTime）
     * <p>适用于评论/回复等需携带 content 与 relatedId 的通知场景。
     *
     * @param receiverId 接收者 ID
     * @param senderId   发送者 ID
     * @param type       通知类型
     * @param title      通知标题
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param relatedId  关联 ID（如帖子 ID）
     * @param content    通知正文
     * @return 已预设 createTime 的 NotificationEvent
     */
    public static NotificationEvent of(Long receiverId, Long senderId, NotificationType type,
                                       String title, String targetType, Long targetId,
                                       Long relatedId, String content) {
        return NotificationEvent.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .type(type.name())
                .title(title)
                .targetType(targetType)
                .targetId(targetId)
                .relatedId(relatedId)
                .content(content)
                .createTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 发布通知事件到 MQ，失败仅记录日志不抛异常
     *
     * @param rabbitTemplate RabbitTemplate 实例
     * @param event          通知事件
     */
    public static void publish(RabbitTemplate rabbitTemplate, NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                NotificationConstants.EXCHANGE,
                NotificationConstants.ROUTING_KEY,
                event
            );
        } catch (Exception e) {
            log.warn("MQ 发布通知失败: type={}, receiverId={}", event.getType(), event.getReceiverId(), e);
        }
    }
}
