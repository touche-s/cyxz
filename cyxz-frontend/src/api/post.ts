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
  id: number | string
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

// 查询分类列表
export const getCategoryList = () => {
  return request.get('/category/list')
}
