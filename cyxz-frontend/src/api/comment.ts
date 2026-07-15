import request from '@/utils/request'

/** 评论 VO */
export interface CommentVO {
  id: string
  postId: string
  postTitle: string
  userId: string
  userName: string
  userAvatar: string
  content: string
  parentId: string | null
  replyToUserId: string | null
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
  postId: string
  content: string
  parentId?: string
  replyToUserId?: string
}

/** 查询顶级评论列表 */
export const getCommentList = (params: {
  postId: string
  page?: number
  size?: number
}) => request.get('/comment/list', { params })

/** 查询子回复列表 */
export const getCommentReplies = (params: {
  parentId: string
  page?: number
  size?: number
}) => request.get('/comment/replies', { params })

/** 发表评论 */
export const createComment = (data: CreateCommentRequest): Promise<{ data: { code: number; message: string; data: CommentVO } }> =>
  request.post('/comment', data)

/** 删除评论 */
export const deleteComment = (commentId: string) =>
  request.delete(`/comment/${commentId}`)

/** 点赞/取消点赞评论 */
export const toggleCommentLike = (commentId: string) =>
  request.post(`/comment/${commentId}/like`)

/** 查询用户收到的评论列表 */
export const getReceivedComments = (params: { page?: number; size?: number }) => {
  return request.get('/comment/received', { params })
}
