package com.cyxz.comment.feign.fallback;

import com.cyxz.comment.feign.CommentFeignClient;
import com.cyxz.common.base.Result;
import com.cyxz.common.feign.AbstractFeignFallbackFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 评论服务 Feign 降级工厂
 * <p>当 cyxz-comment 服务不可用时返回安全默认值，避免调用方 try-catch 模板。
 */
@Component
@ConditionalOnClass(FallbackFactory.class)
public class CommentFeignClientFallbackFactory extends AbstractFeignFallbackFactory<CommentFeignClient> {

    @Override
    protected String serviceName() {
        return "评论服务";
    }

    @Override
    protected CommentFeignClient createFallback(Throwable cause) {
        return new CommentFeignClient() {
            @Override
            public Result<Void> deleteByPostId(Long postId) {
                log.error("评论服务降级，删除帖子关联评论失败: postId={}", postId);
                return Result.fail("评论服务不可用，请稍后重试");
            }

            @Override
            public Result<Integer> countTodayComments(Long postAuthorId) {
                log.warn("评论服务降级，获取今日评论数失败: postAuthorId={}", postAuthorId);
                return Result.success(0);
            }
        };
    }
}
