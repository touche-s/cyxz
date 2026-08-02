import { ref } from 'vue'
import type { ChatMessageVO } from '@/api/chat'

type MessageHandler = (msg: ChatMessageVO) => void
type NotificationHandler = (data: any) => void

/**
 * 全局 WebSocket 连接管理（单例）
 *
 * - 一个用户只维护一个 WS 连接，私信和通知共用
 * - 连接 URL: ws://{host}:8080/ws/message?token={token}
 * - 服务端推送 { type: 'message' | 'notification', data: ... }
 * - 断线 3 秒自动重连
 * - 在 App.vue 顶层建立连接，组件级只注册回调
 */

const connected = ref(false)

let ws: WebSocket | null = null
let reconnectTimer: number | null = null
let manuallyClosed = false
const messageHandlers = new Set<MessageHandler>()
const notificationHandlers = new Set<NotificationHandler>()

function buildUrl(): string {
  const token = localStorage.getItem('token') || ''
  const host = window.location.hostname
  return `ws://${host}:8080/ws/message?token=${encodeURIComponent(token)}`
}

function clearReconnectTimer() {
  if (reconnectTimer != null) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

function scheduleReconnect() {
  if (manuallyClosed) return
  if (reconnectTimer != null) return
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null
    connect()
  }, 3000)
}

/** 建立连接（若已连接或正在连接则跳过） */
function connect() {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return
  }
  manuallyClosed = false
  let socket: WebSocket
  try {
    socket = new WebSocket(buildUrl())
  } catch {
    scheduleReconnect()
    return
  }
  ws = socket
  socket.onopen = () => {
    connected.value = true
  }
  socket.onclose = () => {
    connected.value = false
    if (!manuallyClosed) {
      scheduleReconnect()
    }
  }
  socket.onerror = () => {
    try { socket.close() } catch { /* ignore */ }
  }
  socket.onmessage = (event) => {
    let payload: any
    try {
      payload = JSON.parse(event.data)
    } catch {
      return
    }
    if (!payload || !payload.type) return
    if (payload.type === 'message' && payload.data) {
      messageHandlers.forEach((h) => {
        try { h(payload.data) } catch { /* ignore */ }
      })
    } else if (payload.type === 'notification' && payload.data) {
      notificationHandlers.forEach((h) => {
        try { h(payload.data) } catch { /* ignore */ }
      })
    }
  }
}

/** 主动断开连接，不再自动重连 */
function disconnect() {
  manuallyClosed = true
  clearReconnectTimer()
  if (ws) {
    try { ws.close() } catch { /* ignore */ }
    ws = null
  }
  connected.value = false
}

/** 注册私信消息回调，返回取消注册函数 */
function onMessage(handler: MessageHandler): () => void {
  messageHandlers.add(handler)
  return () => { messageHandlers.delete(handler) }
}

/** 注册通知推送回调，返回取消注册函数 */
function onNotification(handler: NotificationHandler): () => void {
  notificationHandlers.add(handler)
  return () => { notificationHandlers.delete(handler) }
}

export function useChatWebSocket() {
  return { connected, connect, disconnect, onMessage, onNotification }
}
