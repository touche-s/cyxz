package com.cyxz.post.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * post 模块基础 Bean 配置
 * <p>包含 RestTemplate（带超时）与两个职责分离的线程池：
 * <ul>
 *   <li>{@code aiReviewExecutor}：秒级慢 IO（AI 审核 HTTP），小池 + 短队列，CallerRuns 反压</li>
 *   <li>{@code postQueryExecutor}：毫秒级快任务（Feign 并行 + 缓存查询），大池 + 长队列，AbortPolicy 快速失败</li>
 * </ul>
 * 拆分避免 {@link java.util.concurrent.ForkJoinPool#commonPool()} 快慢任务混跑导致雪崩。
 */
@Configuration
public class PostConfig {

    /**
     * AI 审核调用的 RestTemplate，强制超时防止线程被无限阻塞
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * AI 审核异步执行池：慢 IO（秒级 HTTP）
     * <p>core=2 控制并发，max=4 兜底，queue=100 缓冲，CallerRunsPolicy 让提交线程感知反压。
     */
    @Bean("aiReviewExecutor")
    public ExecutorService aiReviewExecutor() {
        return new ThreadPoolExecutor(
                2, 4,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new CustomizableThreadFactory("ai-review-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 帖子详情并行查询池：快任务（Feign + DB，毫秒级）
     * <p>core=8 并发查询作者/圈子/板块/点赞/收藏，max=16 兜底，queue=500 缓冲，
     * CallerRunsPolicy 让线程池满载时由提交线程自己执行，宁慢不报错。
     */
    @Bean("postQueryExecutor")
    public ExecutorService postQueryExecutor() {
        return new ThreadPoolExecutor(
                8, 16,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                new CustomizableThreadFactory("post-query-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
