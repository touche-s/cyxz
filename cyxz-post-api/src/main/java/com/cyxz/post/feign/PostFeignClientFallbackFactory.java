package com.cyxz.post.feign;

import com.cyxz.common.base.Result;
import com.cyxz.post.vo.PostInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 帖子服务 Feign 降级工厂
 * <p>当 cyxz-post 服务不可用时返回安全默认值，避免调用方 try-catch 模板。
 */
@Slf4j
public class PostFeignClientFallbackFactory implements FallbackFactory<PostFeignClient> {

    @Override
    public PostFeignClient create(Throwable cause) {
        log.warn("帖子服务调用降级: {}", cause.getMessage());
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
        };
    }
}
