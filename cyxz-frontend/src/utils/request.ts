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

// ==================== Token 静默续期 ====================

/** 执行刷新并落盘新 Token（不经过拦截器重试逻辑，防止自递归） */
async function doRefreshToken(): Promise<string> {
  const data = await request.post('/auth/refresh', undefined, {
    _skipAuthRefresh: true,
  } as any) as any
  const accessToken: string | undefined = data?.accessToken
  if (!accessToken) throw new Error('刷新响应缺少 accessToken')

  localStorage.setItem('token', accessToken)
  const { useUserStore } = await import('@/stores/user')
  useUserStore().setToken(accessToken)
  // 刷新响应顺带下发最新权限码，保持 RBAC 数据新鲜
  if (Array.isArray(data?.permissions)) {
    useUserStore().setPermissions(data.permissions)
  }
  return accessToken
}

let refreshingPromise: Promise<string> | null = null

/** 单飞刷新：并发失效请求共享同一次刷新，避免重复调 /auth/refresh 导致旧 Token 互相失效 */
function refreshAccessToken(): Promise<string> {
  if (!refreshingPromise) {
    refreshingPromise = doRefreshToken().finally(() => {
      refreshingPromise = null
    })
  }
  return refreshingPromise
}

/** 尝试续期并重放原请求；不可续期（刷新接口自身/已重放过/无 config）返回 null */
async function tryRefreshAndRetry(config: any): Promise<any | null> {
  if (!config || config._skipAuthRefresh || config._retried) return null
  try {
    const newToken = await refreshAccessToken()
    config._retried = true
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${newToken}`
    return await request(config)
  } catch {
    return null
  }
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  async (res) => {
    const body = res.data
    if (body && isSuccessCode(body.code)) {
      return body.data
    }
    // 业务层返回的认证类错误（如 TOKEN_EXPIRED）：先静默续期重放，失败再登出
    if (body && isAuthError(body.code)) {
      const retryRes = await tryRefreshAndRetry(res.config)
      if (retryRes !== null) return retryRes
      handleUnauthorized().catch(() => {})
    }
    return Promise.reject(new ApiError(body?.code ?? -1, body?.message ?? '请求失败'))
  },
  async (err) => {
    if (err instanceof ApiError) {
      return Promise.reject(err)
    }
    // 网关层返回 HTTP 401/403，body 中也携带 6 位 code：同样先尝试续期重放
    const bodyCode = err.response?.data?.code
    const authFailed = (bodyCode && isAuthError(bodyCode)) || err.response?.status === 401
    if (authFailed) {
      const retryRes = await tryRefreshAndRetry(err.config)
      if (retryRes !== null) return retryRes
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
