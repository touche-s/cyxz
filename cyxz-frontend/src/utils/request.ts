import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

let handlingUnauthorized = false

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => res,
  async (err) => {
    if (err.response?.status === 401) {
      if (handlingUnauthorized) {
        return Promise.reject(err)
      }

      handlingUnauthorized = true

      // 延迟导入避免 Pinia 未安装时调用
      const { useUserStore } = await import('@/stores/user')
      useUserStore().clearAuth()
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')

      const currentPath = router.currentRoute.value.path
      if (currentPath !== '/') {
        router.push('/')
      }

      ElMessage.error('登录已过期，请重新登录')

      setTimeout(() => {
        handlingUnauthorized = false
      }, 1000)
    }
    return Promise.reject(err)
  }
)

export default request
