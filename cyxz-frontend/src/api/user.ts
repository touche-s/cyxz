import request from '@/utils/request'

export interface UserInfo {
  userId: string
  nickname: string
  avatar: string
  gender: number
  bio: string
  birthday: string
}

export interface UserProfileVO {
  userId: string
  nickname: string
  avatar: string
  bio: string
  createTime: string
}

export interface FollowUserVO {
  userId: string
  nickname: string
  avatar: string
  bio: string
  createTime: string
  following: boolean
}

export interface FollowStats {
  followingCount: number
  followerCount: number
}

export const getUserProfile = (userId: string) => request.get(`/user/${userId}`)

/** 查询当前登录用户的资料（查不到则自动创建默认资料） */
export const getMyProfile = () => request.get('/user/profile/me')

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
export const isFollowing = (targetUserId: string) => {
  return request.get(`/user/${targetUserId}/is-following`)
}

/** 查询当前用户的关注列表 */
export const getFollowingList = (params: { page?: number; size?: number }) => {
  return request.get('/user/following', { params })
}

/** 查询当前用户的粉丝列表 */
export const getFollowerList = (params: { page?: number; size?: number }) => {
  return request.get('/user/followers', { params })
}

/** 查询当前用户的关注数和粉丝数 */
export const getFollowStats = () => {
  return request.get('/user/follow-stats')
}
