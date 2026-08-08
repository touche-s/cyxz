package com.cyxz.post.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * <p>安全策略：fail-closed —— AI 返回空或序列化失败时拒绝，避免违规内容自动放行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.review.url:http://127.0.0.1:8000/review}")
    private String reviewUrl;

    @Value("${ai.review.image-url:http://127.0.0.1:8000/review/image}")
    private String reviewImageUrl;

    /**
     * 执行审核（文本 + 图片），返回整体结果
     * <p>文本先审，再逐张审图，任一不通过即返回失败。
     *
     * @param postId     帖子 ID（透传给 AI 服务用于日志追踪）
     * @param title      帖子标题
     * @param content    帖子正文
     * @param imageUrls  图片 URL 列表（可为 null）
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
        return doPost(reviewUrl, Map.of(
                "post_id", postId,
                "title", title != null ? title : "",
                "content", content != null ? content : ""
        ));
    }

    private AiReviewResult callImageReview(Long postId, String imageUrl) {
        return doPost(reviewImageUrl, Map.of(
                "post_id", postId,
                "image_url", imageUrl != null ? imageUrl : ""
        ));
    }

    private AiReviewResult doPost(String url, Map<String, Object> payload) {
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("AI 审核请求序列化失败", e);
            return new AiReviewResult(false, "审核服务异常");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity(url, request, AiReviewResult.class);
        AiReviewResult result = response.getBody();
        // fail-closed：AI 返回空时拒绝，避免违规内容自动放行
        return result != null ? result : new AiReviewResult(false, "审核服务异常");
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
