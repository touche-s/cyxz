<template>
  <div class="pm-page" :class="{ 'pm-page--chat-open': isMobile && showChat }">
    <div class="pm-shell">
      <div class="pm-side" v-show="!isMobile || !showChat">
        <div class="pm-side-hd">
          <div class="pm-side-title-row">
            <h2 class="pm-side-title">私信</h2>
            <Icon icon="ph:chat-circle-dots" class="pm-side-title-icon" />
          </div>
          <p class="pm-side-sub">和同好聊聊吧</p>
          <div class="pm-search">
            <Icon icon="ph:magnifying-glass" class="pm-search-icon" />
            <input v-model="searchKeyword" type="text" class="pm-search-input" placeholder="搜索对话..." />
          </div>
        </div>
        <div class="pm-side-label">最近消息</div>
        <div class="pm-conv-list">
          <div
            v-for="conv in filteredConversations"
            :key="conv.id"
            class="pm-conv-item"
            :class="{ active: selectedId === conv.id }"
            @click="selectConv(conv)"
          >
            <img
              v-if="conv.peerAvatar"
              :src="avatarUrl(conv.peerAvatar)"
              alt="avatar"
              class="pm-conv-avatar pm-conv-avatar--img"
            />
            <div v-else class="pm-conv-avatar" :style="{ background: avatarGrad(conv.peerName) }">
              {{ (conv.peerName || 'U').charAt(0) }}
            </div>
            <div class="pm-conv-main">
              <div class="pm-conv-top">
                <span class="pm-conv-name">{{ conv.peerName }}</span>
                <span class="pm-conv-time">{{ formatTime(conv.lastMessageAt) }}</span>
              </div>
              <div class="pm-conv-bottom">
                <span class="pm-conv-preview">{{ conv.lastMessage }}</span>
                <span v-if="conv.unreadCount > 0" class="pm-conv-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
              </div>
            </div>
          </div>
          <div v-if="!loadingConvs && filteredConversations.length === 0" class="pm-conv-empty">
            还没有私信对话
          </div>
        </div>
      </div>

      <div class="pm-chat" v-show="!isMobile || showChat">
        <template v-if="selectedConv">
          <div class="pm-chat-hd">
            <button v-if="isMobile" class="pm-chat-back" @click="showChat = false">
              <Icon icon="ph:arrow-left" />
            </button>
            <img
              v-if="selectedConv.peerAvatar"
              :src="avatarUrl(selectedConv.peerAvatar)"
              alt="avatar"
              class="pm-chat-avatar-sm pm-chat-avatar-sm--img"
            />
            <div v-else class="pm-chat-avatar-sm" :style="{ background: avatarGrad(selectedConv.peerName) }">
              {{ (selectedConv.peerName || 'U').charAt(0) }}
            </div>
            <div class="pm-chat-hd-info">
              <span class="pm-chat-hd-name">{{ selectedConv.peerName }}</span>
            </div>
            <div class="pm-chat-hd-actions">
              <button class="pm-hd-btn"><Icon icon="ph:magnifying-glass" /></button>
              <button class="pm-hd-btn"><Icon icon="ph:dots-three-outline-vertical" /></button>
            </div>
          </div>

          <div class="pm-chat-body" ref="chatBody">
            <div v-if="messages.length > 0" class="pm-time-sep"><span>{{ formatDateLabel(messages[0].createTime) }}</span></div>
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="pm-msg"
              :class="{ 'pm-msg--self': isSelfMessage(msg) }"
            >
              <div class="pm-msg-bubble">
                {{ msg.content }}
              </div>
              <span class="pm-msg-time">{{ formatMsgTime(msg.createTime) }}</span>
            </div>
          </div>

          <div class="pm-chat-ft">
            <div class="pm-ft-tools">
              <button class="pm-ft-btn"><Icon icon="ph:smiley" /></button>
              <button class="pm-ft-btn"><Icon icon="ph:image" /></button>
            </div>
            <div class="pm-ft-input-row">
              <input
                v-model="newMessage"
                type="text"
                class="pm-ft-input"
                placeholder="发一条友善的消息吧~"
                @keydown.enter="handleSend"
              />
              <button
                class="pm-ft-send"
                :class="{ disabled: !newMessage.trim() || sending }"
                @click="handleSend"
              >
                发送
              </button>
            </div>
          </div>
        </template>

        <div v-else class="pm-chat-empty">
          <div class="pm-empty-icon-wrap">
            <Icon icon="ph:envelope-open" class="pm-empty-icon" />
          </div>
          <p class="pm-empty-title">选择一条对话开始聊天</p>
          <p class="pm-empty-desc">这里会显示你与其他用户的私信对话</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import {
  getConversations,
  getMessages,
  sendMessage as apiSendMessage,
  markRead,
} from '@/api/chat'
import type { ConversationVO, ChatMessageVO } from '@/api/chat'
import { useChatWebSocket } from '@/composables/useChatWebSocket'
import { useUserStore } from '@/stores/user'
import { avatarUrl } from '@/utils/avatar'
import { formatTime } from '@/utils/format'

const route = useRoute()
const userStore = useUserStore()
const { onMessage } = useChatWebSocket()

const chatBody = ref<HTMLElement>()

const conversations = ref<ConversationVO[]>([])
const messages = ref<ChatMessageVO[]>([])
const selectedId = ref<number | null>(null)
const newMessage = ref('')
const sending = ref(false)
const loadingConvs = ref(false)
const searchKeyword = ref('')
const isMobile = ref(false)
const showChat = ref(false)

let mqListener: ((e: MediaQueryListEvent) => void) | null = null
let wsOff: (() => void) | null = null

const currentUserId = computed(() =>
  String(userStore.userInfo?.id || userStore.userInfo?.userId || '')
)

const filteredConversations = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return conversations.value
  return conversations.value.filter(
    (c) =>
      (c.peerName || '').toLowerCase().includes(kw) ||
      (c.lastMessage || '').toLowerCase().includes(kw)
  )
})

const selectedConv = computed(
  () => conversations.value.find((c) => c.id === selectedId.value) || null
)

function isSelfMessage(msg: ChatMessageVO): boolean {
  return String(msg.senderId) === currentUserId.value
}

function formatMsgTime(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

function formatDateLabel(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const isToday =
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
  if (isToday) return '今天'
  const yest = new Date(now)
  yest.setDate(now.getDate() - 1)
  const isYesterday =
    d.getFullYear() === yest.getFullYear() &&
    d.getMonth() === yest.getMonth() &&
    d.getDate() === yest.getDate()
  if (isYesterday) return '昨天'
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

// 粉紫色系字母头像渐变（peerAvatar 缺省时使用）
const AVATAR_GRADIENTS = [
  'linear-gradient(135deg, #f9a8d4, #c4b5fd)',
  'linear-gradient(135deg, #a5f3fc, #c4b5fd)',
  'linear-gradient(135deg, #fde68a, #f9a8d4)',
  'linear-gradient(135deg, #f9a8d4, #fda4af)',
  'linear-gradient(135deg, #c084fc, #a78bfa)',
  'linear-gradient(135deg, #ff8ac8, #c084fc)',
]
function avatarGrad(name: string): string {
  if (!name) return AVATAR_GRADIENTS[0]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) >>> 0
  }
  return AVATAR_GRADIENTS[hash % AVATAR_GRADIENTS.length]
}

function scrollToBottom() {
  if (chatBody.value) {
    chatBody.value.scrollTop = chatBody.value.scrollHeight
  }
}

async function loadConversations(): Promise<ConversationVO[]> {
  loadingConvs.value = true
  try {
    const list = await getConversations()
    conversations.value = Array.isArray(list) ? list : []
  } catch {
    ElMessage.error('加载会话列表失败')
    conversations.value = []
  } finally {
    loadingConvs.value = false
  }
  return conversations.value
}

async function loadMessages(convId: number) {
  try {
    const data = await getMessages(convId, { page: 1, size: 50 })
    messages.value = (data?.records || []).reverse()
  } catch {
    messages.value = []
  }
  nextTick(() => scrollToBottom())
}

async function selectConv(conv: ConversationVO) {
  if (!conv) return
  selectedId.value = conv.id
  if (isMobile.value) showChat.value = true
  await loadMessages(conv.id)
  // 进入会话后标记已读
  if (conv.unreadCount > 0) {
    try {
      await markRead(conv.id)
      conv.unreadCount = 0
    } catch { /* ignore */ }
  }
}

async function handleSend() {
  const text = newMessage.value.trim()
  if (!text || sending.value || !selectedConv.value) return
  sending.value = true
  const conv = selectedConv.value
  const tempId = Date.now()
  // 乐观插入
  const optimistic: ChatMessageVO = {
    id: tempId,
    conversationId: conv.id,
    senderId: currentUserId.value,
    receiverId: conv.peerId,
    content: text,
    read: false,
    createTime: new Date().toISOString(),
  }
  messages.value.push(optimistic)
  conv.lastMessage = text
  conv.lastMessageAt = optimistic.createTime
  newMessage.value = ''
  nextTick(() => scrollToBottom())
  try {
    const saved = await apiSendMessage({ receiverId: conv.peerId, content: text })
    if (saved) {
      const idx = messages.value.findIndex((m) => m.id === tempId)
      if (idx >= 0) messages.value.splice(idx, 1, saved)
    }
  } catch {
    ElMessage.error('发送失败，请稍后重试')
    const idx = messages.value.findIndex((m) => m.id === tempId)
    if (idx >= 0) messages.value.splice(idx, 1)
    newMessage.value = text
  } finally {
    sending.value = false
  }
}

function findConvByPeerId(peerId: string): ConversationVO | undefined {
  return conversations.value.find((c) => String(c.peerId) === String(peerId))
}

/** WebSocket 推送处理：当前会话窗口 append + markRead；其它会话未读 +1 */
function handleWsMessage(msg: ChatMessageVO) {
  if (!msg) return
  const convId = msg.conversationId
  const conv = conversations.value.find((c) => c.id === convId)
  // 更新会话最后消息
  if (conv) {
    conv.lastMessage = msg.content
    conv.lastMessageAt = msg.createTime
  }
  if (selectedId.value === convId) {
    // 当前正在该会话窗口
    const exists = messages.value.some((m) => m.id === msg.id)
    if (!exists) {
      messages.value.push(msg)
    }
    nextTick(() => scrollToBottom())
    if (!isSelfMessage(msg)) {
      markRead(convId).catch(() => {})
      if (conv) conv.unreadCount = 0
    }
  } else if (conv && !isSelfMessage(msg)) {
    conv.unreadCount = (conv.unreadCount || 0) + 1
  }
  // 会话列表里没有这条会话（新会话），重新拉取列表
  if (!conv) {
    loadConversations()
  }
}

onMounted(async () => {
  const mq = window.matchMedia('(max-width: 768px)')
  isMobile.value = mq.matches
  mqListener = (e) => { isMobile.value = e.matches }
  mq.addEventListener('change', mqListener)

  await loadConversations()

  // URL query 参数 peerId 优先打开该用户的会话
  const peerIdParam = route.query.peerId
  if (peerIdParam) {
    const target = findConvByPeerId(String(peerIdParam))
    if (target) {
      await selectConv(target)
    } else if (conversations.value.length === 0) {
      ElMessage.info('暂无与该用户的会话记录')
    }
  } else if (conversations.value.length > 0) {
    await selectConv(conversations.value[0])
  }

  // 启动 WebSocket
  wsOff = onMessage(handleWsMessage)
  connect()
})

onUnmounted(() => {
  if (mqListener) {
    window.matchMedia('(max-width: 768px)').removeEventListener('change', mqListener)
    mqListener = null
  }
  if (wsOff) { wsOff(); wsOff = null }
})
</script>

<style scoped>
.pm-page {
  min-height: 100vh;
  padding-top: 80px;
  background: radial-gradient(ellipse at 40% 30%, rgba(255,107,157,0.04) 0%, rgba(180,132,255,0.03) 50%, transparent 70%), var(--bg);
}

.pm-shell {
  max-width: 1060px;
  margin: 0 auto;
  padding: 0 24px 24px;
  display: grid;
  grid-template-columns: 310px 1fr;
  min-height: calc(100vh - 104px);
}

.pm-side {
  background: var(--card);
  border-radius: 22px 0 0 22px;
  border: 1px solid var(--border-light);
  border-right: none;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pm-side-hd {
  padding: 22px 20px 16px;
  border-bottom: 1px solid var(--border-light);
}

.pm-side-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.pm-side-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text);
  margin: 0;
}

.pm-side-title-icon {
  width: 22px;
  height: 22px;
  color: var(--pink);
  opacity: 0.8;
}

.pm-side-sub {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0 0 14px;
}

.pm-search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-soft);
  border-radius: 12px;
  padding: 0 14px;
  height: 38px;
  border: 1px solid transparent;
  transition: border-color 0.2s;
}

.pm-search:focus-within {
  border-color: rgba(255, 107, 157, 0.2);
  background: var(--card);
}

.pm-search-icon {
  width: 16px;
  height: 16px;
  color: var(--text-dim);
  flex-shrink: 0;
}

.pm-search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: var(--text);
  font-family: inherit;
}

.pm-search-input::placeholder {
  color: var(--text-dim);
}

.pm-side-label {
  padding: 14px 20px 6px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-dim);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.pm-conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px 12px;
}

.pm-conv-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.pm-conv-item:hover {
  background: rgba(255, 107, 157, 0.04);
}

.pm-conv-item.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.08), rgba(180, 132, 255, 0.06));
}

.pm-conv-item.active .pm-conv-name {
  color: var(--pink);
  font-weight: 700;
}

.pm-conv-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--white);
  font-size: 18px;
  font-weight: 800;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.pm-conv-avatar--img {
  object-fit: cover;
  color: transparent;
  font-size: 0;
}

.pm-conv-empty {
  padding: 40px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--text-dim);
}

.pm-conv-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 2px;
}

.pm-conv-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pm-conv-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.pm-conv-time {
  font-size: 11px;
  color: var(--text-dim);
  flex-shrink: 0;
}

.pm-conv-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pm-conv-preview {
  font-size: 12px;
  color: var(--text-dim);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

.pm-conv-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--gradient-brand);
  color: var(--white);
  font-size: 10px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  flex-shrink: 0;
}

/* ===== 右侧聊天区 ===== */
.pm-chat {
  background: var(--card);
  border-radius: 0 22px 22px 0;
  border: 1px solid var(--border-light);
  border-left: none;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.pm-chat-hd {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light);
  min-height: 60px;
}

.pm-chat-back {
  display: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--text);
  cursor: pointer;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: background 0.2s;
}

.pm-chat-back:hover {
  background: var(--pink-bg);
}

.pm-chat-avatar-sm {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--white);
  font-size: 15px;
  font-weight: 800;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
}

.pm-chat-avatar-sm--img {
  object-fit: cover;
  color: transparent;
  font-size: 0;
}

.pm-chat-hd-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pm-chat-hd-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.pm-chat-hd-status {
  font-size: 12px;
  color: var(--text-dim);
  display: flex;
  align-items: center;
  gap: 5px;
}

.pm-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--success);
  flex-shrink: 0;
}

.pm-chat-hd-actions {
  display: flex;
  gap: 2px;
}

.pm-hd-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--text-dim);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all 0.2s;
}

.pm-hd-btn:hover {
  background: var(--pink-bg);
  color: var(--pink);
}

/* ===== 消息区 ===== */
.pm-chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background:
    radial-gradient(ellipse at 50% 50%, rgba(255,107,157,0.02) 0%, transparent 70%),
    var(--bg-soft);
}

.pm-time-sep {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
}

.pm-time-sep span {
  font-size: 11px;
  color: var(--text-dim);
  background: var(--card);
  padding: 3px 14px;
  border-radius: 10px;
  border: 1px solid var(--border-light);
}

.pm-msg {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-width: 70%;
}

.pm-msg--self {
  align-self: flex-end;
  align-items: flex-end;
}

.pm-msg:not(.pm-msg--self) {
  align-self: flex-start;
  align-items: flex-start;
}

.pm-msg-bubble {
  padding: 10px 16px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.55;
  word-break: break-word;
}

.pm-msg--self .pm-msg-bubble {
  background: linear-gradient(135deg, #ff8ac8, #c084fc);
  color: var(--white);
  border-bottom-right-radius: 8px;
}

.pm-msg:not(.pm-msg--self) .pm-msg-bubble {
  background: var(--card);
  color: var(--text);
  border: 1px solid var(--border-light);
  border-bottom-left-radius: 8px;
}

.pm-msg-time {
  font-size: 10px;
  color: var(--text-dim);
  padding: 0 4px;
}

/* ===== 输入区 ===== */
.pm-chat-ft {
  border-top: 1px solid var(--border-light);
  padding: 10px 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pm-ft-tools {
  display: flex;
  gap: 4px;
}

.pm-ft-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-dim);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all 0.2s;
}

.pm-ft-btn:hover {
  background: var(--pink-bg);
  color: var(--pink);
}

.pm-ft-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pm-ft-input {
  flex: 1;
  height: 42px;
  padding: 0 16px;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background: var(--bg-soft);
  font-size: 14px;
  color: var(--text);
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s, background 0.2s;
}

.pm-ft-input:focus {
  border-color: rgba(255, 107, 157, 0.3);
  background: var(--card);
}

.pm-ft-input::placeholder {
  color: var(--text-dim);
}

.pm-ft-send {
  padding: 0 22px;
  height: 42px;
  border-radius: 14px;
  border: none;
  background: var(--gradient-brand);
  color: var(--white);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s ease;
  box-shadow: 0 4px 14px rgba(255, 107, 157, 0.22);
  flex-shrink: 0;
}

.pm-ft-send:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.3);
}

.pm-ft-send:active {
  transform: scale(0.97);
}

.pm-ft-send.disabled {
  opacity: 0.5;
  cursor: default;
  transform: none;
  box-shadow: none;
}

/* ===== 空状态 ===== */
.pm-chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 14px;
}

.pm-empty-icon-wrap {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--pink-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.pm-empty-icon {
  width: 34px;
  height: 34px;
  color: var(--pink);
  opacity: 0.6;
}

.pm-empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.pm-empty-desc {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .pm-page {
    padding-top: 78px;
  }

  .pm-shell {
    display: block;
    padding: 0;
    min-height: calc(100vh - 78px);
  }

  .pm-side {
    border-radius: 0;
    border: none;
    height: calc(100vh - 78px);
  }

  .pm-chat {
    border-radius: 0;
    border: none;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 200;
    background: var(--bg);
  }

  .pm-page:not(.pm-page--chat-open) .pm-chat {
    display: none;
  }

  .pm-chat-back {
    display: flex;
  }

  .pm-chat-hd {
    background: var(--card);
    border-bottom: 1px solid var(--border-light);
    padding: 12px 14px;
  }

  .pm-chat-body {
    padding: 12px 14px 8px;
  }

  .pm-msg {
    max-width: 80%;
  }

  .pm-chat-ft {
    padding: 8px 12px 12px;
  }
}
</style>
