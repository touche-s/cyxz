package com.cyxz.post.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 敏感词检测服务（基于 classpath:sensitive_words.txt）
 * <p>发布时做快速拦截，命中则拒绝。深度审核由 AI 服务负责。
 */
@Slf4j
@Service
public class SensitiveWordService {

    private Set<String> words = Collections.emptySet();

    @PostConstruct
    void load() {
        try {
            ClassPathResource resource = new ClassPathResource("sensitive_words.txt");
            Set<String> set = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    set.add(line.toLowerCase());
                }
            }
            words = Collections.unmodifiableSet(set);
            log.info("敏感词库加载完成，共 {} 个词", words.size());
        } catch (Exception e) {
            log.error("敏感词库加载失败，降级为空词库", e);
        }
    }

    /**
     * 检测文本是否包含敏感词
     * @param texts 待检测的文本（可传多个，如标题、正文）
     * @return 命中的敏感词集合，为空表示通过
     */
    public Set<String> check(String... texts) {
        if (words.isEmpty()) return Collections.emptySet();
        Set<String> hits = new LinkedHashSet<>();
        for (String text : texts) {
            if (text == null || text.isEmpty()) continue;
            String lower = text.toLowerCase();
            for (String w : words) {
                if (lower.contains(w)) {
                    hits.add(w);
                }
            }
        }
        return hits;
    }
}
