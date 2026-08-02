import request from '@/utils/request'
import type { PageResult } from '@/api/types/common'

/** 用户信息 */
export interface UserInfo {
  userId: string
  nickname: string
  avatar: string
  gender: number
  bio: string
  birthday: string
  followingCount?: number
  followerCount?: number
}

/** 用户资料 VO */
export interface UserProfileVO {
  userId: string
  nickname: string
  avatar: string
  bio: string
  createTime: string
}

/** 关注用户 VO */
export interface FollowUserVO {
  userId: string
  nickname: string
  avatar: string
  bio: string
  createTime: string
  following: boolean
}

/** 关注统计 */
export interface FollowStats {
  followingCount: number
  followerCount: number
  newFollowerCount: number
}

/** 查询用户资料 */
export const getUserProfile = (userId: string): Promise<UserProfileVO> => request.get(`/user/${userId}`)

/** 查询当前登录用户的资料（查不到则自动创建默认资料） */
export const getMyProfile = (): Promise<UserInfo> => request.get('/user/profile/me')

/** 修改用户资料 */
export const updateUserProfile = (data: Partial<UserInfo>) => request.put('/user/profile', data)

/** 关注用户 */
export const followUser = (targetUserId: string) => {
  return request.post(`/user/${targetUserId}/follow`)
}

/** 取消关注用户 */
export const unfollowUser = (targetUserId: string) => {
  return request.delete(`/user/${targetUserId}/follow`)
}

/** 查询是否关注了某个用户 */
export const isFollowing = (targetUserId: string): Promise<boolean> => {
  return request.get(`/user/${targetUserId}/is-following`)
}

/** 查询当前用户的关注列表 */
export const getFollowingList = (params: { page?: number; size?: number }): Promise<PageResult<FollowUserVO>> => {
  return request.get('/user/following', { params })
}

/** 查询当前用户的粉丝列表 */
export const getFollowerList = (params: { page?: number; size?: number }): Promise<PageResult<FollowUserVO>> => {
  return request.get('/user/followers', { params })
}

/** 查询当前用户的关注数和粉丝数 */
export const getFollowStats = (): Promise<FollowStats> => {
  return request.get('/user/follow-stats') as any
}
