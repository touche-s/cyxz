package com.cyxz.message.feign.fallback;

import com.cyxz.common.base.Result;
import com.cyxz.common.feign.AbstractFeignFallbackFactory;
import com.cyxz.message.dto.CreateNotificationRequest;
import com.cyxz.message.feign.MessageFeignClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 消息服务 Feign 降级工厂
 * <p>当 cyxz-message 服务不可用时返回安全默认值，避免调用方抛异常中断主流程。
 */
@Component
@ConditionalOnClass(FallbackFactory.class)
public class MessageFeignClientFallbackFactory extends AbstractFeignFallbackFactory<MessageFeignClient> {

    @Override
    protected String serviceName() {
        return "消息服务";
    }

    @Override
    protected MessageFeignClient createFallback(Throwable cause) {
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
