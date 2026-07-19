import request from '@/utils/request'

/** 帖子 VO */
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

/** 分类 VO */
export interface CategoryVO {
  id: number
  name: string
  description: string
  sortOrder: number
}

/** 保存草稿请求 */
export interface SaveDraftRequest {
  categoryId?: number
  title?: string
  content?: string
  cover?: string
  images?: string[]
  tags?: string[]
}

/** 发布帖子请求（标题、分类、正文、图片必填） */
export interface PublishPostRequest {
  categoryId: number
  title: string
  content: string
  cover?: string
  images: string[]
  tags?: string[]
}

/** 更新帖子请求 */
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

/** 查询帖子列表（仅已发布，可按分类筛选） */
export const getPostList = (params: { categoryId?: number; page?: number; size?: number }) => {
  return request.get('/post/list', { params })
}

/** 查询帖子详情 */
export const getPostDetail = (postId: string) => {
  return request.get(`/post/${postId}`)
}

/** 发布帖子 */
export const createPost = (data: PublishPostRequest) => {
  return request.post('/post', data)
}

/** 保存草稿 */
export const saveDraftPost = (data: SaveDraftRequest) => {
  return request.post('/post/draft', data)
}

/** 更新帖子 */
export const updatePost = (data: UpdatePostRequest) => {
  return request.put('/post', data)
}

/** 删除帖子（逻辑删除，状态变为已删除，可在回收站恢复） */
export const deletePost = (postId: string) => {
  return request.delete(`/post/${postId}`)
}

/** 彻底删除帖子（物理删除，同时清理关联的评论、点赞、收藏） */
export const permanentDeletePost = (postId: string) => {
  return request.delete(`/post/${postId}/permanent`)
}

/** 查询当前用户的帖子列表（含草稿和已删除，内容管理用） */
export const getUserPosts = (params: { page?: number; size?: number }) => {
  return request.get('/post/user', { params })
}

/** 查询指定用户的已发布帖子列表（个人空间 - 作品 tab） */
export const getUserPostsByTarget = (targetUserId: string, params: { page?: number; size?: number }) => {
  return request.get(`/post/user/${targetUserId}/posts`, { params })
}

/** 查询指定用户的收藏帖子列表（个人空间 - 收藏 tab） */
export const getUserFavorites = (targetUserId: string, params: { page?: number; size?: number }) => {
  return request.get(`/post/user/${targetUserId}/favorites`, { params })
}

/** 查询分类列表 */
export const getCategoryList = () => {
  return request.get('/category/list')
}

/** 点赞帖子 */
export const likePost = (postId: string) => {
  return request.put(`/post/${postId}/like`)
}

/** 取消点赞帖子 */
export const unlikePost = (postId: string) => {
  return request.delete(`/post/${postId}/like`)
}

/** 收藏帖子 */
export const collectPost = (postId: string) => {
  return request.put(`/post/${postId}/collect`)
}

/** 取消收藏帖子 */
export const uncollectPost = (postId: string) => {
  return request.delete(`/post/${postId}/collect`)
}

/** 记录浏览（进入详情页静默上报，失败不影响展示） */
export const recordPostView = (postId: string) => {
  return request.post(`/post/${postId}/view`)
}

/** 帖子统计 VO（数据中心） */
export interface PostStatsVO {
  totalPosts: number
  totalViews: number
  totalLikes: number
  totalCollections: number
}

/** 获取当前用户的帖子统计数据（数据中心） */
export const getPostStats = () => {
  return request.get('/post/stats')
}

/** 获取指定用户的帖子统计数据（个人空间 - 查看他人空间时展示获赞、浏览） */
export const getUserPostStats = (userId: string) => {
  return request.get(`/post/user/${userId}/stats`)
}

/** 获取用户作品排行榜（按浏览量倒序） */
export const getTopPosts = (limit?: number) => {
  return request.get('/post/top', { params: { limit } })
}

/** 收到的点赞 VO */
export interface ReceivedLikeVO {
  likeId: string
  postId: string
  postTitle: string
  userId: string
  userName: string
  userAvatar: string
  createTime: string
}

/** 查询用户收到的点赞列表 */
export const getReceivedLikes = (params: { page?: number; size?: number }) => {
  return request.get('/post/received-likes', { params })
}
