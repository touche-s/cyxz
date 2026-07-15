import request from '@/utils/request'

export interface PostVO {
  id: string
  userId: string
  authorName: string
  authorAvatar: string
  categoryId: number
  categoryName: string
  title: string
  content: string
  cover: string
  images: string[]
  tags: string[]
  status: number
  likes: number
  comments: number
  views: number
  collections: number
  liked: boolean
  collected: boolean
  createTime: string
  updateTime: string
}

export interface CategoryVO {
  id: number
  name: string
  description: string
  sortOrder: number
}

export interface CreatePostRequest {
  categoryId: number
  title: string
  content: string
  cover?: string
  images?: string[]
  tags?: string[]
  status?: number
}

export interface UpdatePostRequest {
  id: string
  categoryId?: number
  title?: string
  content?: string
  cover?: string
  images?: string[]
  tags?: string[]
  status?: number
}

// 查询帖子列表
export const getPostList = (params: { categoryId?: number; page?: number; size?: number }) => {
  return request.get('/post/list', { params })
}

// 查询帖子详情
export const getPostDetail = (postId: string) => {
  return request.get(`/post/${postId}`)
}

// 创建帖子
export const createPost = (data: CreatePostRequest) => {
  return request.post('/post', data)
}

// 更新帖子
export const updatePost = (data: UpdatePostRequest) => {
  return request.put('/post', data)
}

// 删除帖子（软删除，状态变为已删除，可在回收站恢复）
export const deletePost = (postId: string) => {
  return request.delete(`/post/${postId}`)
}

// 查询当前用户的帖子列表（从 Header X-User-Id 获取用户 ID）
export const getUserPosts = (params: { page?: number; size?: number }) => {
  return request.get('/post/user', { params })
}

// 查询指定用户的已发布帖子列表（个人空间 - 作品 tab）
export const getUserPostsByTarget = (targetUserId: string, params: { page?: number; size?: number }) => {
  return request.get(`/post/user/${targetUserId}/posts`, { params })
}

// 查询指定用户的收藏帖子列表（个人空间 - 收藏 tab）
export const getUserFavorites = (targetUserId: string, params: { page?: number; size?: number }) => {
  return request.get(`/post/user/${targetUserId}/favorites`, { params })
}

// 查询分类列表
export const getCategoryList = () => {
  return request.get('/category/list')
}

// 点赞 / 取消点赞帖子
export const togglePostLike = (postId: string) => {
  return request.post(`/post/${postId}/like`)
}

// 收藏 / 取消收藏帖子
export const togglePostCollect = (postId: string) => {
  return request.post(`/post/${postId}/collect`)
}

// 记录浏览（进入详情页静默上报，失败不影响展示）
export const recordPostView = (postId: string) => {
  return request.post(`/post/${postId}/view`)
}

// 帖子统计 VO（数据中心）
export interface PostStatsVO {
  totalPosts: number
  totalViews: number
  totalLikes: number
  totalCollections: number
}

// 获取当前用户的帖子统计数据（数据中心）
export const getPostStats = () => {
  return request.get('/post/stats')
}

// 获取用户作品排行榜（按浏览量倒序）
export const getTopPosts = (limit?: number) => {
  return request.get('/post/top', { params: { limit } })
}

// 收到的点赞 VO
export interface ReceivedLikeVO {
  likeId: string
  postId: string
  postTitle: string
  userId: string
  userName: string
  userAvatar: string
  createTime: string
}

// 查询用户收到的点赞列表
export const getReceivedLikes = (params: { page?: number; size?: number }) => {
  return request.get('/post/received-likes', { params })
}
