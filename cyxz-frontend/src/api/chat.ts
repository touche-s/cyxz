import request from '@/utils/request'
import type { PageResult } from '@/api/types/common'

/** 私信会话 VO */
export interface ConversationVO {
  id: number
  peerId: string
  peerName: string
  peerAvatar: string
  lastMessage: string
  lastMessageAt: string
  unreadCount: number
}

/** 私信消息 VO */
export interface ChatMessageVO {
  id: number
  conversationId: number
  senderId: string
  receiverId: string
  content: string
  read: boolean
  createTime: string
}

/** 发送私信请求 */
export interface SendMessageRequest {
  receiverId: string
  content: string
}

/** WebSocket 推送消息信封 */
export interface WsMessageEnvelope {
  type: 'message'
  data: ChatMessageVO
}

/** 获取私信会话列表 */
export const getConversations = (): Promise<ConversationVO[]> =>
  request.get('/message/conversations')

/** 获取会话历史消息（分页） */
export const getMessages = (
  conversationId: number,
  params: { page?: number; size?: number }
): Promise<PageResult<ChatMessageVO>> =>
  request.get(`/message/conversations/${conversationId}/messages`, { params })

/** 发送私信 */
export const sendMessage = (data: SendMessageRequest): Promise<ChatMessageVO> =>
  request.post('/message/send', data)

/** 标记会话已读 */
export const markRead = (conversationId: number): Promise<void> =>
  request.put(`/message/conversations/${conversationId}/read`)

/** 获取私信总未读数 */
export const getUnreadTotal = (): Promise<number> =>
  request.get('/message/unread-total')

/** 查询是否与目标用户互相关注（ProfilePage 私信按钮入口用） */
export const isMutualFollowing = (targetUserId: string): Promise<boolean> =>
  request.get(`/user/${targetUserId}/is-mutual-following`)
