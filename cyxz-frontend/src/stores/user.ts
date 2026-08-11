import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/api/user'

interface StoredUserInfo extends UserInfo {
  id?: string
  role?: string
  permissions?: string[]
}

/** 平台级管理员角色：可进入 /admin 平台管理后台 */
const PLATFORM_ADMIN_ROLES = ['SITE_OWNER', 'PLATFORM_ADMIN']

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<StoredUserInfo | null>(
    JSON.parse(localStorage.getItem('userInfo') || 'null')
  )
  const permissions = ref<string[]>(
    JSON.parse(localStorage.getItem('permissions') || '[]')
  )
  const showLoginModal = ref(false)
  const creatorActiveNav = ref('home')
  const creatorFansTab = ref<'followers' | 'following'>('followers')
  const pendingCircleId = ref<number | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  /** 是否为平台管理员（站主/平台管理员），控制 /admin 入口 */
  const isAdmin = computed(() => PLATFORM_ADMIN_ROLES.includes(userInfo.value?.role || ''))
  /** 当前全局角色 code */
  const role = computed(() => userInfo.value?.role || '')

  function openLoginModal() {
    showLoginModal.value = true
  }

  function closeLoginModal() {
    showLoginModal.value = false
  }

  function setToken(val: string) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function setUserInfo(info: StoredUserInfo) {
    userInfo.value = { ...info, id: info.userId }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  /** 设置全局权限码列表（登录/刷新时调用） */
  function setPermissions(perms: string[]) {
    permissions.value = perms || []
    localStorage.setItem('permissions', JSON.stringify(permissions.value))
  }

  /** 校验当前用户是否拥有指定全局权限码 */
  function hasPermission(code: string): boolean {
    if (isAdmin.value) return true
    return permissions.value.includes(code)
  }

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('permissions')
  }

  return {
    token, userInfo, permissions, isLoggedIn, isAdmin, role,
    showLoginModal, creatorActiveNav, creatorFansTab, pendingCircleId,
    openLoginModal, closeLoginModal, setToken, setUserInfo, setPermissions,
    hasPermission, clearAuth,
  }
})
