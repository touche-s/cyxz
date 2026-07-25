package com.cyxz.message.api.feign;

import com.cyxz.common.base.Result;
import com.cyxz.message.api.dto.CreateNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 消息服务 Feign 客户端
 * <p>供其他微服务通过内部接口创建通知消息，如点赞通知、评论通知、关注通知等。
 */
@FeignClient(name = "cyxz-message", path = "/message", fallbackFactory = MessageFeignClientFallbackFactory.class)
public interface MessageFeignClient {

    /**
     * 创建通知（内部接口）
     * <p>供其他服务在业务操作后异步发送通知消息。
     *
     * @param request 创建通知请求
     * @return 操作结果
     */
    @PostMapping("/internal/notifications")
    Result<Void> createNotification(@RequestBody CreateNotificationRequest request);
}
