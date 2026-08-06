import { useUserStore } from '@/stores/user'

/**
 * 权限校验 composable
 * <p>基于全局角色与权限码做 UI 显隐控制。
 * - 平台管理员（SITE_OWNER/PLATFORM_ADMIN）默认放行所有权限码
 * - 圈子内权限由后端 @circlePerm 实时校验，前端只做入口显隐
 */
export function usePermission() {
  const userStore = useUserStore()

  /** 是否为平台管理员（站主/平台管理员） */
  function isPlatformAdmin(): boolean {
    return userStore.isAdmin
  }

  /** 是否拥有指定全局权限码（平台管理员默认放行） */
  function hasPermission(code: string): boolean {
    return userStore.hasPermission(code)
  }

  /** 是否拥有传入权限码中的任意一个 */
  function hasAnyPermission(codes: string[]): boolean {
    if (userStore.isAdmin) return true
    return codes.some((c) => userStore.permissions.includes(c))
  }

  return { isPlatformAdmin, hasPermission, hasAnyPermission }
}
