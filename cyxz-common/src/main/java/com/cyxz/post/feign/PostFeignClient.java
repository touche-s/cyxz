package com.cyxz.post.feign;

import com.cyxz.common.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * 帖子服务 Feign 客户端
 * <p>供其他微服务（如 comment 服务）通过内部接口调用帖子服务。
 */
@FeignClient(name = "cyxz-post", path = "/post")
public interface PostFeignClient {

    /**
     * 获取帖子作者 ID（内部接口）
     * <p>用于评论服务创建评论时获取帖子作者，以便写入 post_author_id 冗余字段。
     *
     * @param postId 帖子 ID
     * @return 帖子作者用户 ID
     */
    @GetMapping("/internal/{postId}/author")
    Result<Long> getPostAuthor(@PathVariable("postId") Long postId);

    /**
     * 获取帖子信息（内部接口）
     * <p>用于评论服务批量查询帖子标题，填充收到的评论列表。
     *
     * @param postId 帖子 ID
     * @return 帖子信息（postId, userId, title）
     */
    @GetMapping("/internal/{postId}/info")
    Result<Map<String, Object>> getPostInfo(@PathVariable("postId") Long postId);
}