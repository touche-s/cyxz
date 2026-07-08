import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
  captcha: string
  captchaUuid: string
}

export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  captcha: string
  captchaUuid: string
}

export const getCaptcha = () => request.get('/auth/captcha')

export const login = (data: LoginRequest) => request.post('/auth/login', data)

export const register = (data: RegisterRequest) => request.post('/auth/register', data)

export const logout = () => request.post('/auth/logout')
