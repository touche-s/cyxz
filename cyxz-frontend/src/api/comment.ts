import request from '@/utils/request'

/** 评论 VO */
export interface CommentVO {
  id: number
  postId: number
  userId: number
  userName: string
  userAvatar: string
  content: string
  parentId: number | null
  replyToUserId: number | null
  replyToUserName: string | null
  likes: number
  liked: boolean
  children: CommentVO[]
  totalReplies: number | null
  hasMoreReplies: boolean | null
  createTime: string
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 发表评论参数 */
export interface CreateCommentRequest {
  postId: number
  content: string
  parentId?: number
  replyToUserId?: number
}

/** 查询顶级评论列表 */
export const getCommentList = (params: {
  postId: number
  page?: number
  size?: number
}) => request.get('/comment/list', { params })

/** 查询子回复列表 */
export const getCommentReplies = (params: {
  parentId: number
  page?: number
  size?: number
}) => request.get('/comment/replies', { params })

/** 发表评论（返回完整 CommentVO，含用户信息，前端可直接插入列表） */
export const createComment = (data: CreateCommentRequest): Promise<{ data: { code: number; message: string; data: CommentVO } }> =>
  request.post('/comment', data)

/** 删除评论 */
export const deleteComment = (commentId: number) =>
  request.delete(`/comment/${commentId}`)

/** 点赞/取消点赞评论 */
export const toggleCommentLike = (commentId: number) =>
  request.post(`/comment/${commentId}/like`)

/** 查询用户收到的评论列表（对用户帖子的评论） */
export const getReceivedComments = (params: { page?: number; size?: number }) => {
  return request.get('/comment/received', { params })
}
