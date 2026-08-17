import request from '@/utils/request'
import type { PageResult } from '@/api/types/common'

/** 通知 VO */
export interface NotificationVO {
  id: number
  senderId: number
  senderName: string
  senderAvatar: string
  type: string
  actionText: string
  targetId: number
  targetType: string
  relatedId: number
  targetTitle: string
  content: string
  isRead: boolean
  createTime: string
}

/** 获取通知列表 */
export const getNotifications = (params: { type?: string; page?: number; size?: number }): Promise<PageResult<NotificationVO>> =>
  request.get('/message/notifications', { params })

/** 获取未读数量 */
export const getUnreadCount = (): Promise<number> =>
  request.get('/message/unread-count') as any

/** 标记单条已读 */
export const markRead = (id: number) =>
  request.put(`/message/${id}/read`)

/** 全部已读 */
export const markAllRead = () =>
  request.put('/message/read-all')
