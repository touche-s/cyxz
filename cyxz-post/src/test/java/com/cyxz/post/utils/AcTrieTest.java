package com.cyxz.post.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AC 自动机单元测试
 * <p>覆盖：基本命中、fail 指针跨词命中、多词同时命中、大小写归一、无命中、边界值。
 */
@DisplayName("AcTrie — AC 自动机多模式串匹配")
class AcTrieTest {

    @Test
    @DisplayName("单词命中：文本包含敏感词返回命中")
    void shouldHitSingleWord() {
        AcTrie trie = new AcTrie(Collections.singleton("敏感"));

        Set<String> hits = trie.scan("这是敏感内容");

        assertEquals(1, hits.size());
        assertTrue(hits.contains("敏感"));
    }

    @Test
    @DisplayName("fail 指针跨词命中：shers 同时命中 she 和 hers")
    void shouldHitBothWordsViaFailPointer() {
        // 经典案例：she 和 hers 共享后缀 "he"/"ers"，
        // 扫描 "shers" 时通过 fail 指针从 she 跳到 he 继续匹配 hers
        AcTrie trie = new AcTrie(Arrays.asList("she", "hers"));

        Set<String> hits = trie.scan("shers");

        assertEquals(2, hits.size());
        assertTrue(hits.contains("she"));
        assertTrue(hits.contains("hers"));
    }

    @Test
    @DisplayName("多词同时命中：一次扫描返回所有命中")
    void shouldHitAllWordsInOneScan() {
        AcTrie trie = new AcTrie(Arrays.asList("abc", "bcd", "xyz"));

        Set<String> hits = trie.scan("zzabczzbcdzzxyz");

        assertEquals(3, hits.size());
        assertTrue(hits.contains("abc"));
        assertTrue(hits.contains("bcd"));
        assertTrue(hits.contains("xyz"));
    }

    @Test
    @DisplayName("无命中：返回空集合")
    void shouldReturnEmptyWhenNoMatch() {
        AcTrie trie = new AcTrie(Arrays.asList("abc", "def"));

        Set<String> hits = trie.scan("zzzggg");

        assertTrue(hits.isEmpty());
    }

    @Test
    @DisplayName("空/Null 文本：返回空集合不抛异常")
    void shouldHandleNullOrEmptyText() {
        AcTrie trie = new AcTrie(Arrays.asList("abc"));

        assertDoesNotThrow(() -> trie.scan(null));
        assertTrue(trie.scan(null).isEmpty());
        assertTrue(trie.scan("").isEmpty());
    }

    @Test
    @DisplayName("空词库：任何文本都不命中")
    void shouldNeverHitWithEmptyDictionary() {
        AcTrie trie = new AcTrie(Collections.emptySet());

        assertTrue(trie.scan("任意内容").isEmpty());
    }

    @Test
    @DisplayName("敏感词为子串：命中短词不漏长词")
    void shouldHitBothShortAndLongWhenOverlapping() {
        // "ab" 和 "abc" 共享前缀，扫描 "xabcx" 应同时命中
        AcTrie trie = new AcTrie(Arrays.asList("ab", "abc"));

        Set<String> hits = trie.scan("xabcx");

        assertEquals(2, hits.size());
        assertTrue(hits.contains("ab"));
        assertTrue(hits.contains("abc"));
    }

    @Test
    @DisplayName("词尾边界：敏感词正好在文本末尾")
    void shouldHitWhenWordAtEnd() {
        AcTrie trie = new AcTrie(Collections.singleton("abc"));

        Set<String> hits = trie.scan("zzabc");

        assertEquals(1, hits.size());
        assertTrue(hits.contains("abc"));
    }

    @Test
    @DisplayName("词首边界：敏感词正好在文本开头")
    void shouldHitWhenWordAtStart() {
        AcTrie trie = new AcTrie(Collections.singleton("abc"));

        Set<String> hits = trie.scan("abczz");

        assertEquals(1, hits.size());
        assertTrue(hits.contains("abc"));
    }

    @Test
    @DisplayName("重复命中：同一敏感词多次出现只记一次")
    void shouldDeduplicateRepeatedHits() {
        AcTrie trie = new AcTrie(Collections.singleton("abc"));

        Set<String> hits = trie.scan("abcabcabc");

        assertEquals(1, hits.size());
        assertTrue(hits.contains("abc"));
    }
}
