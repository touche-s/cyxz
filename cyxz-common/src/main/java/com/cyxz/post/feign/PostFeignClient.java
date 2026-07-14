package com.cyxz.post.feign;

import com.cyxz.common.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "cyxz-post", path = "/api/post")
public interface PostFeignClient {

    @GetMapping("/internal/{postId}/author")
    Result<Long> getPostAuthor(@PathVariable("postId") Long postId);

    @GetMapping("/internal/{postId}/info")
    Result<Map<String, Object>> getPostInfo(@PathVariable("postId") Long postId);
}