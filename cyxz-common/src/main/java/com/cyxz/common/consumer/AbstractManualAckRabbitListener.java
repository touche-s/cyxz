package com.cyxz.common.consumer;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 手动 ACK RabbitMQ 消费者抽象基类
 * <p>统一 try/handle/catch/basicReject/basicAck 模板，子类只需实现 {@link #handle} 与 {@link #describe}。
 *
 * @param <T> 事件类型
 */
public abstract class AbstractManualAckRabbitListener<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 子类实现具体业务处理逻辑
     *
     * @param event 事件
     * @throws Exception 业务异常，将触发拒绝入死信
     */
    protected abstract void handle(T event) throws Exception;

    /**
     * 子类返回事件描述用于失败日志（如 type/postId 等）
     *
     * @param event 事件
     * @return 日志描述字符串
     */
    protected abstract String describe(T event);

    /**
     * 模板方法：handle → 成功 ACK / 失败拒绝入死信
     * <p>失败时不重新入队（requeue=false），消息进入死信队列避免丢失。
     *
     * @param event   事件
     * @param channel RabbitMQ Channel
     * @param tag     消息 delivery tag
     * @throws IOException ACK / Reject 时抛出
     */
    protected final void processWithManualAck(T event, Channel channel, long tag) throws IOException {
        try {
            handle(event);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("事件消费失败，进入死信: {}", describe(event), e);
            channel.basicReject(tag, false);
        }
    }
}
