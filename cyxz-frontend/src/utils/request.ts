import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { isSuccessCode, isAuthError } from '@/utils/errorCode'

export class ApiError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'ApiError'
  }
}

let handlingUnauthorized = false

/** 统一处理登录失效：清空登录态、跳转首页、提示（1 秒内去重） */
async function handleUnauthorized(): Promise<void> {
  if (handlingUnauthorized) return
  handlingUnauthorized = true

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
  (res) => {
    const body = res.data
    if (body && isSuccessCode(body.code)) {
      return body.data
    }
    // 业务层返回的认证类错误（如 TOKEN_EXPIRED），统一走登出逻辑
    if (body && isAuthError(body.code)) {
      handleUnauthorized().catch(() => {})
    }
    return Promise.reject(new ApiError(body?.code ?? -1, body?.message ?? '请求失败'))
  },
  async (err) => {
    if (err instanceof ApiError) {
      return Promise.reject(err)
    }
    // 网关层返回 HTTP 401/403，body 中也携带 6 位 code
    const bodyCode = err.response?.data?.code
    if (bodyCode && isAuthError(bodyCode)) {
      await handleUnauthorized()
    } else if (err.response?.status === 401) {
      await handleUnauthorized()
    }
    const msg = err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new ApiError(bodyCode ?? -1, msg))
  },
)

export default request as unknown as {
  get<R = any>(url: string, config?: any): Promise<R>
  post<R = any>(url: string, data?: any, config?: any): Promise<R>
  put<R = any>(url: string, data?: any, config?: any): Promise<R>
  delete<R = any>(url: string, config?: any): Promise<R>
}
