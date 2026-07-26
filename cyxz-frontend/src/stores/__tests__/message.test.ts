import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useMessageStore } from '@/stores/message'

// mock getUnreadCount API
vi.mock('@/api/message', () => ({
  getUnreadCount: vi.fn(),
}))

import { getUnreadCount } from '@/api/message'

describe('useMessageStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('refreshUnreadCount', () => {
    it('拉取成功时更新未读数', async () => {
      vi.mocked(getUnreadCount).mockResolvedValue(5)
      const store = useMessageStore()

      await store.refreshUnreadCount()

      expect(store.unreadCount).toBe(5)
    })

    it('返回 0 时应正确设置为 0', async () => {
      vi.mocked(getUnreadCount).mockResolvedValue(0)
      const store = useMessageStore()
      store.unreadCount = 3 // 先设一个非零值

      await store.refreshUnreadCount()

      expect(store.unreadCount).toBe(0)
    })

    it('API 失败时保持当前值不变且不抛错', async () => {
      vi.mocked(getUnreadCount).mockRejectedValue(new Error('网络错误'))
      const store = useMessageStore()
      store.unreadCount = 3

      await store.refreshUnreadCount()

      expect(store.unreadCount).toBe(3)
    })
  })

  describe('clearUnreadCount', () => {
    it('清零后未读数为 0', () => {
      const store = useMessageStore()
      store.unreadCount = 10

      store.clearUnreadCount()

      expect(store.unreadCount).toBe(0)
    })
  })

  describe('状态隔离', () => {
    it('不同 Pinia 实例之间未读数互不影响', () => {
      const store1 = useMessageStore()
      store1.unreadCount = 5

      // 创建新 Pinia 实例模拟另一个组件的 store
      setActivePinia(createPinia())
      const store2 = useMessageStore()

      expect(store2.unreadCount).toBe(0)
    })
  })
})
