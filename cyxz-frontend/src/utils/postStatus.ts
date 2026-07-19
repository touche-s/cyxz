/**
 * 帖子状态常量与工具方法
 * 统一帖子状态判断、文案映射和发布前校验，避免在各页面中散落 status === 0/1/2
 */

export const POST_STATUS = {
  DRAFT: 0,
  PUBLISHED: 1,
  DELETED: 2,
} as const

export type PostStatus = (typeof POST_STATUS)[keyof typeof POST_STATUS]

export const POST_STATUS_TEXT: Record<number, string> = {
  [POST_STATUS.DRAFT]: '草稿',
  [POST_STATUS.PUBLISHED]: '已发布',
  [POST_STATUS.DELETED]: '已删除',
}

export const isDraft = (status: number | null | undefined): boolean =>
  status === POST_STATUS.DRAFT

export const isPublished = (status: number | null | undefined): boolean =>
  status === POST_STATUS.PUBLISHED

export const isDeleted = (status: number | null | undefined): boolean =>
  status === POST_STATUS.DELETED

/**
 * 根据状态值返回中文文案
 */
export const statusText = (status: number): string =>
  POST_STATUS_TEXT[status] ?? '未知'

/**
 * 前端发布前校验：是否满足发布必要条件
 * 发布必要条件：标题 + 分类 + 正文 + 至少一张图片
 */
export const canPublish = (post: {
  title?: string
  categoryId?: number | string
  content?: string
  images?: string[]
}): boolean =>
  !!post.title?.trim() &&
  !!post.categoryId &&
  !!post.content?.trim() &&
  Array.isArray(post.images) &&
  post.images.length > 0

/**
 * 合法的帖子状态迁移
 * 键 = 当前状态，值 = 允许迁入的目标状态列表
 */
export const ALLOWED_TRANSITIONS: Record<number, number[]> = {
  [POST_STATUS.DRAFT]: [POST_STATUS.PUBLISHED, POST_STATUS.DELETED],
  [POST_STATUS.PUBLISHED]: [POST_STATUS.DRAFT, POST_STATUS.DELETED],
  [POST_STATUS.DELETED]: [POST_STATUS.DRAFT],
}

export const canTransition = (
  from: number | null | undefined,
  to: number,
): boolean => {
  if (from == null) return true
  return (ALLOWED_TRANSITIONS[from] ?? []).includes(to)
}
