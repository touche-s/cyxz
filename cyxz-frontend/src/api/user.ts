import request from '@/utils/request'

export interface UserInfo {
  userId: number
  nickname: string
  avatar: string
  gender: number
  bio: string
  birthday: string
}

export const getUserProfile = (userId: number) => request.get(`/user/${userId}`)

export const updateUserProfile = (data: Partial<UserInfo>) => request.put('/user/profile', data)
