package com.cyxz.audit.consumer;

import com.cyxz.audit.api.constant.AuditConstants;
import com.cyxz.audit.api.event.AuditEvent;
import com.cyxz.audit.entity.AuditLogPO;
import com.cyxz.audit.mapper.AuditLogMapper;
import com.cyxz.common.consumer.AbstractManualAckRabbitListener;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 审计事件消费者
 * <p>消费各业务服务发布的 {@link AuditEvent}，将其转换为 {@link AuditLogPO} 落库，
 * 实现关键操作的统一留痕。处理失败的消息进入死信队列避免丢失。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer extends AbstractManualAckRabbitListener<AuditEvent> {

    private final AuditLogMapper auditLogMapper;

    @RabbitListener(queues = AuditConstants.QUEUE, ackMode = "MANUAL")
    public void onEvent(AuditEvent event, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        processWithManualAck(event, channel, tag);
    }

    /**
     * 死信监听：记录日志与报警，不再重试
     */
    @RabbitListener(queues = AuditConstants.DLQ)
    public void onDeadLetter(AuditEvent event) {
        log.error("审计事件死信: action={}, operatorId={}, targetType={}, targetId={}",
                event.getAction(), event.getOperatorId(), event.getTargetType(), event.getTargetId());
    }

    @Override
    protected void handle(AuditEvent event) {
        AuditLogPO po = new AuditLogPO();
        po.setOperatorId(event.getOperatorId());
        po.setOperatorName(event.getOperatorName());
        po.setAction(event.getAction());
        po.setTargetType(event.getTargetType());
        po.setTargetId(event.getTargetId());
        po.setDetail(event.getDetail());
        po.setIp(event.getIp());
        po.setEventId(event.getEventId());
        // event_id 唯一约束防重复消费
        try {
            auditLogMapper.insert(po);
        } catch (DuplicateKeyException e) {
            log.debug("审计事件重复跳过: eventId={}, action={}", event.getEventId(), event.getAction());
            return;
        }
        log.debug("审计日志落库: eventId={}, action={}, operatorId={}", event.getEventId(), event.getAction(), event.getOperatorId());
    }

    @Override
    protected String describe(AuditEvent event) {
        return "AuditEvent(action=" + event.getAction()
                + ", operatorId=" + event.getOperatorId()
                + ", targetId=" + event.getTargetId() + ")";
    }
}
