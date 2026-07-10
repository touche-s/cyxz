import request from '@/utils/request'

export interface PostVO {
  id: number
  userId: number
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
  id: number
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
export const getPostDetail = (postId: number) => {
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

// 删除帖子
export const deletePost = (postId: number) => {
  return request.delete(`/post/${postId}`)
}

// 查询用户帖子列表
export const getUserPosts = (userId: number, params: { page?: number; size?: number }) => {
  return request.get(`/post/user/${userId}`, { params })
}

// 查询分类列表
export const getCategoryList = () => {
  return request.get('/category/list')
}
