package com.cyxz.comment.feign.fallback;

import com.cyxz.comment.feign.CommentFeignClient;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 评论服务 Feign 降级工厂
 * <p>当 cyxz-comment 服务不可用时返回安全默认值，避免调用方 try-catch 模板。
 */
@Slf4j
@Component
@ConditionalOnClass(FallbackFactory.class)
public class CommentFeignClientFallbackFactory implements FallbackFactory<CommentFeignClient> {

    @Override
    public CommentFeignClient create(Throwable cause) {
        log.warn("评论服务调用降级: {}", cause.getMessage());
        return new CommentFeignClient() {
            @Override
            public Result<Void> deleteByPostId(Long postId) {
                log.error("评论服务降级，删除帖子关联评论失败: postId={}", postId);
                return Result.fail(ErrorCode.FAIL.getCode(), "评论服务不可用，请稍后重试");
            }

            @Override
            public Result<Integer> countTodayComments(Long postAuthorId) {
                log.warn("评论服务降级，获取今日评论数失败: postAuthorId={}", postAuthorId);
                return Result.success(0);
            }
        };
    }
}
