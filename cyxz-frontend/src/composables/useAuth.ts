import { useUserStore } from '@/stores/user'

export function useAuth() {
  const userStore = useUserStore()

  /**
   * 检查是否已登录，未登录则弹出登录弹窗并返回 false
   * 用法：if (!requireLogin()) return
   */
  function requireLogin(): boolean {
    if (!userStore.isLoggedIn) {
      userStore.openLoginModal()
      return false
    }
    return true
  }

  return { requireLogin, isLoggedIn: userStore.isLoggedIn }
}
