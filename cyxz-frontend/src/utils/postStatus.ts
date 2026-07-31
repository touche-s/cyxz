/**
 * 帖子状态常量与工具方法
 * 0=草稿 1=待审核 2=已通过(公开) 3=拒绝 4=已删除
 */

export const POST_STATUS = {
  DRAFT: 0,
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3,
  DELETED: 4,
} as const

export type PostStatus = (typeof POST_STATUS)[keyof typeof POST_STATUS]

export const POST_STATUS_TEXT: Record<number, string> = {
  [POST_STATUS.DRAFT]: '草稿',
  [POST_STATUS.PENDING]: '待审核',
  [POST_STATUS.APPROVED]: '已通过',
  [POST_STATUS.REJECTED]: '拒绝',
  [POST_STATUS.DELETED]: '已删除',
}

export const isDraft = (status: number | null | undefined): boolean =>
  status === POST_STATUS.DRAFT

export const isPending = (status: number | null | undefined): boolean =>
  status === POST_STATUS.PENDING

export const isApproved = (status: number | null | undefined): boolean =>
  status === POST_STATUS.APPROVED

export const isRejected = (status: number | null | undefined): boolean =>
  status === POST_STATUS.REJECTED

export const isDeleted = (status: number | null | undefined): boolean =>
  status === POST_STATUS.DELETED

/** 兼容旧调用：已发布 = 已通过 */
export const isPublished = (status: number | null | undefined): boolean =>
  isApproved(status)

/** 根据状态值返回中文文案 */
export const statusText = (status: number): string =>
  POST_STATUS_TEXT[status] ?? '未知'

/** 前端发布前校验：图文帖必须标题+正文+至少一张图片 */
export const canPublish = (post: {
  title?: string
  content?: string
  images?: string[]
}): boolean =>
  !!post.title?.trim() &&
  !!post.content?.trim() &&
  Array.isArray(post.images) &&
  post.images.length > 0

/**
 * 用户侧允许的状态迁移
 * 键 = 当前状态，值 = 允许迁入的目标状态列表
 */
export const ALLOWED_TRANSITIONS: Record<number, number[]> = {
  [POST_STATUS.DRAFT]: [POST_STATUS.PENDING, POST_STATUS.DELETED],
  [POST_STATUS.APPROVED]: [POST_STATUS.DELETED],
  [POST_STATUS.REJECTED]: [POST_STATUS.DRAFT],
  [POST_STATUS.DELETED]: [POST_STATUS.DRAFT],
}

export const canTransition = (
  from: number | null | undefined,
  to: number,
): boolean => {
  if (from == null) return true
  return (ALLOWED_TRANSITIONS[from] ?? []).includes(to)
}
