import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

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
  (err) => {
    if (err.response?.status === 401) {
      // 延迟导入避免 Pinia 未安装时调用
      import('@/stores/user').then(({ useUserStore }) => {
        useUserStore().clearAuth()
      })
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      const currentPath = router.currentRoute.value.path
      if (currentPath !== '/') {
        router.push('/')
      }
      ElMessage.error('登录已过期，请重新登录')
    }
    return Promise.reject(err)
  }
)

export default request
