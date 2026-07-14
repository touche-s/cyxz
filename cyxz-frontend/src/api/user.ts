import request from '@/utils/request'

export interface UserInfo {
  userId: string
  nickname: string
  avatar: string
  gender: number
  bio: string
  birthday: string
}

export const getUserProfile = (userId: string | number) => request.get(`/user/${userId}`)

/** 查询当前登录用户的资料（查不到则自动创建默认资料） */
export const getMyProfile = () => request.get('/user/profile/me')

export const updateUserProfile = (data: Partial<UserInfo>) => request.put('/user/profile', data)
