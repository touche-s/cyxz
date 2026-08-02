package com.cyxz.message.feign.fallback;

import com.cyxz.common.base.Result;
import com.cyxz.message.dto.CreateNotificationRequest;
import com.cyxz.message.feign.MessageFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 消息服务 Feign 降级工厂
 * <p>当 cyxz-message 服务不可用时返回安全默认值，避免调用方抛异常中断主流程。
 */
@Slf4j
@Component
@ConditionalOnClass(FallbackFactory.class)
public class MessageFeignClientFallbackFactory implements FallbackFactory<MessageFeignClient> {

    /**
     * 创建消息服务 Feign 降级实例，通知发送失败时记录日志并返回 fail 结果。
     *
     * @param cause 降级原因
     * @return 降级后的 MessageFeignClient 实现
     */
    @Override
    public MessageFeignClient create(Throwable cause) {
        return new MessageFeignClient() {
            @Override
            public Result<Void> createNotification(CreateNotificationRequest request) {
                log.error("消息服务调用失败: type={}, receiverId={}, senderId={}",
                    request.getType(), request.getReceiverId(), request.getSenderId(), cause);
                return Result.fail("消息服务暂不可用");
            }
        };
    }
}
