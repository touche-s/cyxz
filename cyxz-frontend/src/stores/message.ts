import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '@/api/message'

/**
 * 消息未读数全局状态
 * <p>HeaderBar 的铃铛徽标与 MessageCenter 的已读操作共享同一份未读数，
 * 避免两者状态不同步（例如进入消息中心已读后，铃铛要等 30s 轮询才会消失）。
 */
export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)

  /** 从后端拉取最新未读数 */
  async function refreshUnreadCount() {
    try {
      const count: any = await getUnreadCount()
      unreadCount.value = typeof count === 'number' ? count : (count?.data ?? count ?? 0)
    } catch {
      // 忽略未读数拉取失败
    }
  }

  /** 本地清零未读数（已读操作后立即生效，不等下次轮询） */
  function clearUnreadCount() {
    unreadCount.value = 0
  }

  return { unreadCount, refreshUnreadCount, clearUnreadCount }
})
