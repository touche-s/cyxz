import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/api/user'

interface StoredUserInfo extends UserInfo {
  id?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<StoredUserInfo | null>(
    JSON.parse(localStorage.getItem('userInfo') || 'null')
  )
  const showLoginModal = ref(false)
  const creatorActiveNav = ref('home')
  const creatorFansTab = ref<'followers' | 'following'>('followers')
  const pendingCircleId = ref<number | null>(null)

  const isLoggedIn = computed(() => !!token.value)

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

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, isLoggedIn, showLoginModal, creatorActiveNav, creatorFansTab, pendingCircleId, openLoginModal, closeLoginModal, setToken, setUserInfo, clearAuth }
})
