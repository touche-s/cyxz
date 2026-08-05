import request from '@/utils/request'
import type { PageResult } from '@/api/types/common'

/** 帖子 VO */
export interface PostVO {
  id: string
  userId: string
  authorName: string
  authorAvatar: string
  postType: string
  sectionId: number
  sectionName: string
  circleId: number
  circleName: string
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
  pinned: boolean
  pinnedTime: string
  createTime: string
  updateTime: string
}

/** 保存草稿请求 */
export interface SaveDraftRequest {
  postType?: string
  sectionId?: number
  circleId?: number
  title?: string
  content?: string
  cover?: string
  images?: string[]
  tags?: string[]
}

/** 发布帖子请求 */
export interface PublishPostRequest {
  postType?: string
  sectionId?: number
  circleId: number
  title: string
  content: string
  cover?: string
  images: string[]
  tags?: string[]
}

/** 更新帖子请求 */
export interface UpdatePostRequest {
  id: string
  postType?: string
  sectionId?: number
  circleId?: number
  title?: string
  content?: string
  cover?: string
  images?: string[]
  tags?: string[]
  status?: number
}

/** 查询帖子列表（仅已发布，可按板块、圈子筛选，支持排序） */
export const getPostList = (params: { sectionId?: number; circleId?: number; sortBy?: string; page?: number; size?: number }): Promise<PageResult<PostVO>> => {
  return request.get('/post/list', { params })
}

/** 查询关注动态（已加入圈子的帖子） */
export const getFollowingPosts = (params: { page?: number; size?: number }): Promise<PageResult<PostVO>> => {
  return request.get('/post/following', { params })
}

/** 查询帖子详情 */
export const getPostDetail = (postId: string): Promise<PostVO> => {
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
export const getUserPosts = (params: { page?: number; size?: number; sortField?: string; sortOrder?: string }): Promise<PageResult<PostVO>> => {
  return request.get('/post/user', { params })
}

/** 查询指定用户的已发布帖子列表（个人空间 - 作品 tab） */
export const getUserPostsByTarget = (targetUserId: string, params: { page?: number; size?: number }): Promise<PageResult<PostVO>> => {
  return request.get(`/post/user/${targetUserId}/posts`, { params })
}

/** 查询指定用户的收藏帖子列表（个人空间 - 收藏 tab） */
export const getUserFavorites = (targetUserId: string, params: { page?: number; size?: number }): Promise<PageResult<PostVO>> => {
  return request.get(`/post/user/${targetUserId}/favorites`, { params })
}

/** 数据中心板块分布 */
export interface SectionDistributionVO {
  name: string
  count: number
}

/** 数据中心仪表盘 */
export interface DashboardVO {
  summary: PostStatsVO
  monthlyTrends: MonthlyTrendVO[]
  dailyTrends: DailyTrendVO[]
  sectionDistribution: SectionDistributionVO[]
  topPosts: PostVO[]
}
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
export const getPostStats = (): Promise<PostStatsVO> => {
  return request.get('/post/stats')
}

/** 获取指定用户的帖子统计数据（个人空间 - 查看他人空间时展示获赞、浏览） */
export const getUserPostStats = (userId: string): Promise<PostStatsVO> => {
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

/** 数据中心月度趋势 */
export interface MonthlyTrendVO {
  month: string
  posts: number
  views: number
  likes: number
}

/** 数据中心每日趋势 */
export interface DailyTrendVO {
  date: string
  posts: number
  views: number
  likes: number
}

/** 数据中心板块分布 */
export interface SectionDistributionVO {
  name: string
  count: number
}

/** 数据中心仪表盘 */
export interface DashboardVO {
  summary: PostStatsVO
  monthlyTrends: MonthlyTrendVO[]
  dailyTrends: DailyTrendVO[]
  sectionDistribution: SectionDistributionVO[]
  topPosts: PostVO[]
}

/** 获取数据中心仪表盘数据 */
export const getDashboard = (): Promise<DashboardVO> => {
  return request.get('/post/dashboard')
}

/** 今日统计 */
export interface TodayStats {
  todayLikes: number
  todayCollections: number
  todayComments: number
}

/** 获取今日新增互动统计 */
export const getTodayStats = () => {
  return request.get('/post/today')
}

/** 置顶帖子 */
export const pinPost = (postId: string) => {
  return request.put(`/post/${postId}/pin`)
}

/** 取消置顶帖子 */
export const unpinPost = (postId: string) => {
  return request.delete(`/post/${postId}/pin`)
}

/** 批量操作帖子 */
export const batchOperate = (data: { postIds: string[]; action: string }) => {
  return request.post('/post/batch', data)
}

// ===== 管理后台审核 =====

/** 待审核帖子列表 */
export const listPendingReview = (params: { page?: number; size?: number }): Promise<PageResult<PostVO>> => {
  return request.get('/post/admin/review/pending', { params })
}

/** 审核通过 */
export const approvePost = (postId: string) => {
  return request.put(`/post/admin/review/${postId}/approve`)
}

/** 审核拒绝 */
export const rejectPost = (postId: string, reason: string) => {
  return request.put(`/post/admin/review/${postId}/reject`, { reason })
}

/** 敏感词检测，返回命中的敏感词列表 */
export const checkSensitive = (data: { title: string; content: string }): Promise<string[]> => {
  return request.post('/post/check-sensitive', data)
}
