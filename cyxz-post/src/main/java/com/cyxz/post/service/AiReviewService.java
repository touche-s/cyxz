package com.cyxz.post.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * AI 审核服务（纯函数，只调 Python AI，不碰 DB/缓存/MQ）
 * <p>调用方负责处理审核结果：改状态、发通知等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final RestTemplate restTemplate;

    @Value("${ai.review.url:http://127.0.0.1:8000/review}")
    private String reviewUrl;

    @Value("${ai.review.image-url:http://127.0.0.1:8000/review/image}")
    private String reviewImageUrl;

    /**
     * 执行审核（文本 + 图片），返回整体结果
     * <p>文本先审，再逐张审图，任一不通过即返回失败。
     *
     * @return 审核结果，passed=true 表示文本和所有图片均审核通过
     */
    public AiReviewResult review(Long postId, String title, String content, List<String> imageUrls) {
        // 1. 文本审核
        AiReviewResult textResult = callTextReview(postId, title, content);
        if (!textResult.passed) return textResult;

        // 2. 图片审核（逐张）
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                AiReviewResult imgResult = callImageReview(postId, imageUrl);
                if (!imgResult.passed) return imgResult;
            }
        }

        return new AiReviewResult(true, "");
    }

    private AiReviewResult callTextReview(Long postId, String title, String content) {
        String body = String.format(
                "{\"post_id\":%d,\"title\":\"%s\",\"content\":\"%s\"}",
                postId, escape(title), escape(content)
        );
        return doPost(reviewUrl, body);
    }

    private AiReviewResult callImageReview(Long postId, String imageUrl) {
        String body = String.format(
                "{\"post_id\":%d,\"image_url\":\"%s\"}",
                postId, escape(imageUrl)
        );
        return doPost(reviewImageUrl, body);
    }

    private AiReviewResult doPost(String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity(url, request, AiReviewResult.class);
        return response.getBody() != null ? response.getBody() : new AiReviewResult(true, "");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiReviewResult {
        @JsonProperty("passed")
        public boolean passed;
        @JsonProperty("reason")
        public String reason;
    }
}
