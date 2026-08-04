package com.cyxz.post.feign.fallback;

import com.cyxz.common.base.Result;
import com.cyxz.common.feign.AbstractFeignFallbackFactory;
import com.cyxz.post.feign.PostFeignClient;
import com.cyxz.post.vo.PostInfoVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 帖子服务 Feign 降级工厂
 * <p>当 cyxz-post 服务不可用时返回安全默认值，避免调用方 try-catch 模板。
 */
@Component
@ConditionalOnClass(FallbackFactory.class)
public class PostFeignClientFallbackFactory extends AbstractFeignFallbackFactory<PostFeignClient> {

    @Override
    protected String serviceName() {
        return "帖子服务";
    }

    @Override
    protected PostFeignClient createFallback(Throwable cause) {
        return new PostFeignClient() {
            @Override
            public Result<Long> getPostAuthor(Long postId) {
                return Result.success((Long) null);
            }

            @Override
            public Result<Map<String, Object>> getPostInfo(Long postId) {
                return Result.success(Collections.emptyMap());
            }

            @Override
            public Result<List<PostInfoVO>> batchGetPostInfo(Set<Long> postIds) {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<Map<Long, Integer>> batchCountByCircle(Set<Long> circleIds) {
                return Result.success(Collections.emptyMap());
            }
        };
    }
}
