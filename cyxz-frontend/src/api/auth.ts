import request from '@/utils/request'

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
  captcha: string
  captchaUuid: string
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  captcha: string
  captchaUuid: string
}

/** 获取图形验证码 */
export const getCaptcha = () => request.get('/auth/captcha')

/** 登录 */
export const login = (data: LoginRequest) => request.post('/auth/login', data)

/** 注册 */
export const register = (data: RegisterRequest) => request.post('/auth/register', data)

/** 退出登录 */
export const logout = () => request.post('/auth/logout')
