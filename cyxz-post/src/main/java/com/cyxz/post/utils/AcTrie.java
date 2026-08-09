package com.cyxz.post.utils;

import java.util.*;

/**
 * AC 自动机（Aho-Corasick）多模式串匹配。
 * <p>构建 Trie + fail 指针后，扫描文本一次即可返回所有命中的敏感词，
 * 复杂度 O(N)，相比暴力 contains 的 O(N×L×M) 避免了文本被反复扫描。
 * <p>线程安全策略：构建完成后只读扫描线程安全；热更新通过 volatile 引用替换实现。
 */
public class AcTrie {

    /** Trie 节点 */
    private static class Node {
        private final Map<Character, Node> children = new HashMap<>();
        /** fail 指针：失配时跳转到的节点 */
        private Node fail;
        /** 该节点是否为某个敏感词的词尾 */
        private boolean end;
        /** 词尾对应的敏感词（end=true 时非空） */
        private String word;
    }

    private final Node root = new Node();

    /**
     * 批量插入敏感词，构建 Trie 与 fail 指针。
     * @param words 敏感词集合（统一小写）
     */
    public AcTrie(Collection<String> words) {
        for (String w : words) {
            if (w == null || w.isEmpty()) continue;
            insert(w);
        }
        buildFail();
    }

    private void insert(String word) {
        Node cur = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Node next = cur.children.get(c);
            if (next == null) {
                next = new Node();
                cur.children.put(c, next);
            }
            cur = next;
        }
        cur.end = true;
        cur.word = word;
    }

    /**
     * BFS 构建 fail 指针：
     * <ul>
     *   <li>根的子节点 fail 都指向根</li>
     *   <li>其他节点 fail = 父节点.fail 的对应子节点，若无则继续沿 fail 链回退到根</li>
     * </ul>
     */
    private void buildFail() {
        Queue<Node> queue = new ArrayDeque<>();
        // 第一层：fail 指向 root
        for (Node child : root.children.values()) {
            child.fail = root;
            queue.offer(child);
        }
        while (!queue.isEmpty()) {
            Node cur = queue.poll();
            for (Map.Entry<Character, Node> e : cur.children.entrySet()) {
                char c = e.getKey();
                Node child = e.getValue();
                // 沿父节点的 fail 链找第一个有 c 子节点的祖先
                Node f = cur.fail;
                while (f != root && !f.children.containsKey(c)) {
                    f = f.fail;
                }
                child.fail = f.children.getOrDefault(c, root);
                if (child.fail == child) child.fail = root;
                child.end = child.end || child.fail.end;
                // 保留原始 word：若 child 本身是词尾，word 已在 insert 时设置；
                // 若仅通过 fail 继承 end，word 可能为空，扫描时按 fail 链单独收集
                queue.offer(child);
            }
        }
    }

    /**
     * 扫描文本，返回所有命中的敏感词。
     * @param text 待检测文本（调用方负责小写化）
     * @return 命中的敏感词集合（空集合表示未命中）
     */
    public Set<String> scan(String text) {
        Set<String> hits = new LinkedHashSet<>();
        if (text == null || text.isEmpty()) return hits;
        Node cur = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // 失配：沿 fail 链回退，直到找到有 c 子节点的节点或回到 root
            while (cur != root && !cur.children.containsKey(c)) {
                cur = cur.fail;
            }
            Node next = cur.children.get(c);
            cur = (next != null) ? next : root;
            // 沿当前节点的 fail 链收集所有命中的敏感词
            Node t = cur;
            while (t != root) {
                if (t.word != null) {
                    hits.add(t.word);
                }
                t = t.fail;
            }
        }
        return hits;
    }
}
