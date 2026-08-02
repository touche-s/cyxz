package com.cyxz.common.constant;

import java.util.Map;
import java.util.Set;

/**
 * 帖子状态常量与流转规则
 * <p>所有服务共享，避免状态码硬编码导致不一致。
 * 0=草稿 1=待审核 2=已通过(公开) 3=拒绝 4=已删除
 */
public final class PostStatus {

    private PostStatus() {}

    public static final int DRAFT = 0;
    public static final int PENDING = 1;
    public static final int APPROVED = 2;
    public static final int REJECTED = 3;
    public static final int DELETED = 4;

    /** 合法的状态流转表：当前状态 → 允许迁入的目标状态列表
     * <p>任意状态均可软删除（DELETED），符合"用户随时可删自己帖子"的业务直觉。
     */
    private static final Map<Integer, Set<Integer>> ALLOWED_TRANSITIONS = Map.of(
            DRAFT, Set.of(PENDING, DELETED),
            PENDING, Set.of(APPROVED, REJECTED, DRAFT, DELETED),
            APPROVED, Set.of(DELETED),
            REJECTED, Set.of(DRAFT, DELETED),
            DELETED, Set.of(DRAFT)
    );

    private static final Map<Integer, String> STATUS_LABEL = Map.of(
            DRAFT, "草稿",
            PENDING, "待审核",
            APPROVED, "已通过",
            REJECTED, "拒绝",
            DELETED, "已删除"
    );

    public static boolean canTransition(int from, int to) {
        if (from == to) return true;
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static String label(int status) {
        return STATUS_LABEL.getOrDefault(status, "未知");
    }
}
