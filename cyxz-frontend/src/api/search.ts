import request from '@/utils/request'
import type { PageResult } from '@/api/types/common'

export interface PostSearchVO {
  id: number
  userId: number
  circleId: number
  sectionId: number
  postType: string
  title: string
  content: string
  cover: string
  tags: string[]
  status: number
  likes: number
  comments: number
  views: number
  collections: number
  createTime: number
}

/** ES 全文搜索帖子 */
export const searchPostsEs = (params: {
  keyword: string
  circleId?: number
  sortBy?: 'hot' | 'time'
  page?: number
  size?: number
}): Promise<PageResult<PostSearchVO>> => {
  return request.get('/search/post', { params })
}
