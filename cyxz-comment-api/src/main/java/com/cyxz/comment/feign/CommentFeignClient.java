package com.cyxz.comment.feign;

import com.cyxz.common.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 评论服务 Feign 客户端
 * <p>供其他微服务通过内部接口调用评论服务。
 */
@FeignClient(name = "cyxz-comment", path = "/comment", fallbackFactory = CommentFeignClientFallbackFactory.class)
public interface CommentFeignClient {

    /**
     * 删除帖子下的所有评论及评论点赞（内部接口）
     * <p>用于帖子彻底删除时级联清理关联数据。
     *
     * @param postId 帖子 ID
     * @return 操作结果
     */
    @DeleteMapping("/internal/post/{postId}")
    Result<Void> deleteByPostId(@PathVariable("postId") Long postId);
}
