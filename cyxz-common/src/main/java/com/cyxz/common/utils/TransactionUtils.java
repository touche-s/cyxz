package com.cyxz.common.utils;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务工具
 * <p>统一封装"事务提交后执行"的回调注册，避免业务代码散落匿名内部类样板，
 * 也避免在无事务上下文时误抛 IllegalStateException。
 */
public final class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * 在当前事务提交后执行动作；若不存在事务上下文则立即执行。
     * <p>用于 Redis 缓存删除、MQ 消息发送等不参与 DB 事务的副作用，保证 DB 回滚时不会产生幻象副作用。
     *
     * @param action 提交后执行的动作
     */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
