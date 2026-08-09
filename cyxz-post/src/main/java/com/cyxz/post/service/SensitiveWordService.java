package com.cyxz.post.service;

import com.cyxz.post.utils.AcTrie;
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
 * <p>采用 AC 自动机多模式串匹配，扫描文本一次即可返回所有命中敏感词，
 * 复杂度 O(N)。深度审核由 AI 服务负责。
 * <p>词库加载后构建不可变 AcTrie，热更新通过 volatile 引用替换实现。
 */
@Slf4j
@Service
public class SensitiveWordService {

    /** volatile 保证多线程可见性，热更新时整体替换引用 */
    private volatile AcTrie trie = new AcTrie(Collections.emptySet());

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
            trie = new AcTrie(set);
            log.info("敏感词库加载完成，共 {} 个词", set.size());
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
        Set<String> hits = new LinkedHashSet<>();
        AcTrie snapshot = trie;
        for (String text : texts) {
            if (text == null || text.isEmpty()) continue;
            hits.addAll(snapshot.scan(text.toLowerCase()));
        }
        return hits;
    }
}
