<template>
  <div class="message-center">
    <div class="mc-container">
      <!-- 左侧分类导航 -->
      <aside class="mc-sidebar">
        <div class="sidebar-header">
          <h2>消息中心</h2>
        </div>
        <nav class="sidebar-nav">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="nav-item"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <span class="nav-label">{{ tab.label }}</span>
            <span class="nav-badge" v-if="tab.count > 0">{{ tab.count > 99 ? '99+' : tab.count }}</span>
          </button>
        </nav>
      </aside>

      <div class="mc-main-wrap">
        <main class="mc-main">
          <div class="main-header">
            <h3>{{ currentTabLabel }}</h3>
            <button class="mark-all-btn" @click="handleMarkAllRead">全部已读</button>
          </div>

          <!-- 分区渲染 -->
          <template v-for="(section, si) in displaySections" :key="si">
            <div class="section-header" v-if="section.title">
              <span class="section-title">{{ section.title }}</span>
              <span class="section-count" v-if="section.title === '最新'">{{ section.items.length }}</span>
            </div>
            <div class="message-list" v-if="section.items.length > 0">
              <div
                v-for="msg in section.items"
                :key="msg._merged ? 'merged-' + msg.type + '-' + msg.targetId : msg.id"
                class="message-item"
                :class="{ unread: !msg.isRead, merged: msg._merged }"
                @click="handleItemClick(msg)"
              >
                <div class="msg-avatar-col">
                  <div class="msg-avatar merged-avatar" v-if="msg._merged">
                    <div class="avatar-stack">
                      <img
                        v-for="(av, ai) in msg.mergeAvatars"
                        :key="ai"
                        class="stacked-avatar"
                        :src="avatarUrl(av)"
                        alt=""
                        :style="{ left: Number(ai) * 14 + 'px', zIndex: 10 - Number(ai) }"
                      />
                    </div>
                  </div>
                  <div class="msg-avatar" v-else @click.stop="goUser(msg.senderId)">
                    <img :src="avatarUrl(msg.senderAvatar)" alt="" />
                  </div>
                </div>
                <div class="msg-content">
                  <div class="msg-main">
                    <div class="msg-top">
                      <template v-if="msg._merged">
                        <span class="msg-username merged-names" :title="msg.mergeNames">{{ msg.mergeNames }}</span>
                        <span class="msg-action" v-if="msg.actionText">{{ msg.actionText }}</span>
                      </template>
                      <template v-else>
                        <span class="msg-username" @click.stop="goUser(msg.senderId)">{{ msg.senderName }}</span>
                        <span class="msg-action" v-if="msg.actionText">{{ msg.actionText }}</span>
                      </template>
                      <span class="msg-target" v-if="msg.targetTitle" @click.stop="goTarget(msg)">{{ msg.targetTitle }}</span>
                      <span class="msg-dot" v-if="!msg.isRead"></span>
                    </div>
                    <div class="msg-meta-row">
                      <div class="msg-time">{{ msg.timeText }}</div>
                    </div>
                  </div>
                  <div class="msg-side" v-if="msg.quoteContent || msg.targetTitle">
                    <div class="msg-quote" v-if="msg.quoteContent">
                      <span class="quote-text">{{ msg.quoteContent }}</span>
                    </div>
                    <div class="msg-side-title" v-else-if="msg.targetTitle" @click.stop="goTarget(msg)">
                      {{ msg.targetTitle }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- 空状态 -->
          <div class="empty-state" v-if="isEmpty">
            <div class="empty-icon">
              <Icon icon="ph:bell" class="empty-iconify pink-icon" />
            </div>
            <p class="empty-text">暂无{{ currentTabLabel }}消息</p>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useNavigate } from '@/composables/useNavigate'
import { useMessageStore } from '@/stores/message'
import { getNotifications, markAllRead, markRead, type NotificationVO } from '@/api/message'
import { avatarUrl } from '@/utils/avatar'

const { open } = useNavigate()
const messageStore = useMessageStore()

const activeTab = ref('like')
const notifications = ref<NotificationVO[]>([])
const loading = ref(false)

// 类型到前端的映射
const typeConfig: Record<string, { label: string; frontType: string }> = {
  POST_LIKED: { label: '赞了你的帖子', frontType: 'like' },
  POST_COMMENTED: { label: '评论了你的帖子', frontType: 'comment' },
  COMMENT_REPLIED: { label: '回复了你的评论', frontType: 'reply' },
  POST_COLLECTED: { label: '收藏了你的帖子', frontType: 'collect' },
  USER_FOLLOWED: { label: '关注了你', frontType: 'follow' },
}

// 分类计数（mapNotification 已把 type 转成了 frontType）
const commentUnread = computed(() => notifications.value.filter(m => (m.type === 'comment' || m.type === 'reply') && !m.isRead).length)
const likeUnread = computed(() => notifications.value.filter(m => m.type === 'like' && !m.isRead).length)
const collectUnread = computed(() => notifications.value.filter(m => m.type === 'collect' && !m.isRead).length)
const followUnread = computed(() => notifications.value.filter(m => m.type === 'follow' && !m.isRead).length)

const tabs = computed(() => [
  { key: 'like', label: '我收到的赞', count: likeUnread.value },
  { key: 'comment', label: '评论和回复', count: commentUnread.value },
  { key: 'collect', label: '收藏', count: collectUnread.value },
  { key: 'follow', label: '关注', count: followUnread.value },
])

const currentTabLabel = computed(() => {
  const map: Record<string, string> = {
    like: '我收到的赞', comment: '评论和回复',
    collect: '收藏', follow: '关注',
  }
  return map[activeTab.value] || '消息'
})

/** 点赞/收藏 tab 需要分区展示 */
const mergeTab = computed(() => ['like', 'collect'].includes(activeTab.value))

const filteredMessages = computed(() => {
  if (activeTab.value === 'comment') return notifications.value.filter(m => m.type === 'comment' || m.type === 'reply')
  return notifications.value.filter(m => m.type === activeTab.value)
})

/** 合并同类型同目标的点赞/收藏通知 */
function mergeMessages(list: any[]) {
  const groups = new Map<string, any[]>()
  const ungrouped: any[] = []

  for (const msg of list) {
    if (msg.targetId) {
      const key = `${msg.type}_${msg.targetId}`
      if (!groups.has(key)) groups.set(key, [])
      groups.get(key)!.push(msg)
    } else {
      ungrouped.push(msg)
    }
  }

  const result: any[] = []
  for (const [, items] of groups) {
    if (items.length === 0) continue
    // 只有一个人不合并，直接当单条
    if (items.length === 1) {
      result.push(items[0])
      continue
    }
    const first = items[0]
    const names = items.map((m: any) => m.senderName || '用户')
    const top = names.slice(0, 2)
    const suffix = `等总计${items.length}人`
    result.push({
      _merged: true,
      mergeCount: items.length,
      mergeNames: top.join('、') + ' ' + suffix,
      mergeAvatars: items.slice(0, 2).map((m: any) => m.senderAvatar || ''),
      ...first,
      senderName: top.join('、') + ' ' + suffix,
      isRead: items.every((m: any) => m.isRead),
    })
  }
  result.push(...ungrouped)
  result.sort((a, b) => {
    const ta = a.createTime ? new Date(a.createTime).getTime() : 0
    const tb = b.createTime ? new Date(b.createTime).getTime() : 0
    return tb - ta
  })
  return result
}

/** 最新未读（逐条） */
const latestMessages = computed(() => {
  return filteredMessages.value
    .filter(m => !m.isRead)
    .sort((a, b) => {
      const ta = a.createTime ? new Date(a.createTime).getTime() : 0
      const tb = b.createTime ? new Date(b.createTime).getTime() : 0
      return tb - ta
    })
})

/** 累计（合并） */
const accumulatedMessages = computed(() => mergeMessages(filteredMessages.value))

/** 展示分区：
 *  - 点赞/收藏 tab：有未读则 [最新] + [累计]，没有则仅 [累计]
 *  - 其它 tab：直接展示 filteredMessages
 */
const displaySections = computed(() => {
  if (!mergeTab.value) {
    return filteredMessages.value.length > 0
      ? [{ title: null as string | null, items: filteredMessages.value }]
      : []
  }
  const sections: Array<{ title: string | null; items: any[] }> = []
  if (latestMessages.value.length > 0) {
    sections.push({ title: '最新', items: latestMessages.value })
  }
  if (accumulatedMessages.value.length > 0) {
    sections.push({ title: sections.length > 0 ? '累计' : null, items: accumulatedMessages.value })
  }
  return sections
})

const isEmpty = computed(() => displaySections.value.every(s => s.items.length === 0))

/** 同步铃铛未读数为当前全部未读总数 */
function syncStoreUnread() {
  messageStore.unreadCount = notifications.value.filter(m => !m.isRead).length
}

/** 首次加载全部通知（不按类型筛选，确保各 tab 未读数准确） */
async function loadNotifications() {
  loading.value = true
  try {
    let res: any = await getNotifications({ page: 1, size: 20 })
    if (res.records) {
      notifications.value = res.records.map(mapNotification)
    } else {
      const data = res?.data || res
      if (data?.records) {
        notifications.value = data.records.map(mapNotification)
      }
    }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function mapNotification(n: NotificationVO): NotificationVO & { timeText: string } {
  const cfg = typeConfig[n.type] || { label: '与你互动', frontType: 'system' }
  return {
    ...n,
    actionText: cfg.label,
    senderName: n.senderName || '用户',
    senderAvatar: n.senderAvatar || '',
    isRead: n.isRead,
    timeText: formatTime(n.createTime),
    type: cfg.frontType as any,
  } as any
}

function formatTime(time: string): string {
  if (!time) return ''
  const now = Date.now()
  const t = new Date(time).getTime()
  const diff = now - t
  if (diff < 60_000) return '刚刚'
  if (diff < 3600_000) return `${Math.floor(diff / 60_000)}分钟前`
  if (diff < 86400_000) return `${Math.floor(diff / 3600_000)}小时前`
  if (diff < 172800_000) return '昨天'
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 点击消息条目：单条已读 + 导航 */
async function handleItemClick(msg: any) {
  if (!msg.isRead) {
    try {
      await markRead(msg.id)
      const found = notifications.value.find((m: any) => m.id === msg.id)
      if (found) (found as any).isRead = true
      syncStoreUnread()
    } catch { /* ignore */ }
  }
  if (!msg._merged) {
    goTarget(msg)
  }
}

async function handleMarkAllRead() {
  try {
    await markAllRead()
    notifications.value.forEach(m => { m.isRead = true })
    messageStore.clearUnreadCount()
  } catch { /* ignore */ }
}

function goUser(userId: number) {
  if (userId) open(`/user/${userId}`)
}

function goTarget(msg: any) {
  if (msg.targetType === 'post' && msg.targetId) {
    open(`/post/${msg.targetId}`)
  }
  if (msg.relatedId && msg.targetType === 'comment') {
    open(`/post/${msg.relatedId}`)
  }
}

onMounted(async () => {
  await loadNotifications()
})
</script>

<style scoped>
.message-center {
  min-height: calc(100vh - 78px);
  padding-top: 78px;
  background: var(--bg-soft);
}

.mc-container {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 24px 28px 0;
  display: grid;
  grid-template-columns: 220px 760px;
  justify-content: start;
  gap: 0;
  min-height: calc(100vh - 122px);
}

.mc-main-wrap {
  display: flex;
  justify-content: center;
}

/* ===== 左侧导航 ===== */
.mc-sidebar {
  background: var(--card);
  border-radius: 16px;
  border: 1.5px solid var(--border-light);
  box-shadow: var(--shadow);
  height: fit-content;
  overflow: hidden;
}

.sidebar-header {
  padding: 20px 20px 16px;
  border-bottom: 1px solid var(--border-light);
}

.sidebar-header h2 {
  font-size: 18px;
  font-weight: 800;
  color: var(--text);
  margin: 0;
  letter-spacing: 0.5px;
}

.sidebar-nav {
  padding: 8px 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-dim);
  transition: all 0.18s ease;
  width: 100%;
  text-align: left;
  position: relative;
}

.nav-item:hover {
  color: var(--pink);
  background: var(--pink-bg);
}

.nav-item.active {
  color: var(--pink);
  font-weight: 700;
  background: var(--pink-bg);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, var(--pink), var(--purple));
}

.nav-label {
  line-height: 1;
}

.nav-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 700;
  color: white;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  line-height: 1;
}

/* ===== 右侧内容区 ===== */
.mc-main {
  width: 680px;
  max-width: 100%;
  background: var(--card);
  border-radius: 16px;
  border: 1.5px solid var(--border-light);
  box-shadow: var(--shadow);
  overflow: hidden;
  margin: 0 auto;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px;
  border-bottom: 1px solid var(--border-light);
}

.main-header h3 {
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

/* 分区标题 */
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px 4px;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--pink);
  letter-spacing: 0.3px;
}

.section-count {
  font-size: 11px;
  font-weight: 700;
  color: white;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  min-width: 16px;
  height: 16px;
  padding: 0 5px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.mark-all-btn {
  padding: 6px 16px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--pink);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mark-all-btn:hover {
  background: var(--pink-bg);
  border-color: var(--pink);
}

/* ===== 消息列表 ===== */
.message-list {
  padding: 4px 0;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 24px;
  transition: background 0.15s ease;
  cursor: pointer;
}

.message-item:hover {
  background: rgba(255, 107, 157, 0.03);
}

.message-item + .message-item {
  border-top: 1px solid rgba(255, 107, 157, 0.06);
}

.message-item.unread {
  background: rgba(255, 107, 157, 0.02);
}

.msg-avatar-col {
  width: 52px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.msg-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  transition: opacity 0.15s ease;
  border: 1px solid rgba(255, 107, 157, 0.12);
  background: #fff;
}

.msg-avatar:hover {
  opacity: 0.84;
}

.msg-avatar:not(.merged-avatar) img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 堆叠头像 */
.msg-avatar.merged-avatar {
  width: 52px;
  height: 40px;
  border: none;
  border-radius: 0;
  overflow: visible;
  cursor: default;
  background: transparent;
}

.avatar-stack {
  position: relative;
  width: 100%;
  height: 32px;
}

.stacked-avatar {
  position: absolute;
  top: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--card);
  object-fit: cover;
  display: block;
  background: #fff;
}

.merged-names {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: default;
}

.msg-content {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.msg-main {
  flex: 1;
  min-width: 0;
}

.msg-top {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  line-height: 1.6;
}

.msg-username {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  cursor: pointer;
  transition: color 0.15s;
}

.msg-username:hover {
  color: var(--pink);
}

.msg-action {
  font-size: 14px;
  color: var(--text-dim);
}

.msg-target {
  font-size: 14px;
  color: var(--text-dim);
  cursor: pointer;
  transition: color 0.15s;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 260px;
}

.msg-target:hover {
  color: var(--pink);
}

.msg-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  flex-shrink: 0;
  margin-left: 4px;
}

.msg-meta-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 6px;
}

.msg-time {
  font-size: 13px;
  color: var(--text-muted);
}

.msg-side {
  width: 112px;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
}

.msg-quote,
.msg-side-title {
  width: 112px;
  padding: 8px 10px;
  background: var(--pink-bg);
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--text-dim);
  text-align: left;
  word-break: break-word;
}

.msg-side-title {
  cursor: pointer;
  transition: color 0.15s ease, background 0.15s ease;
}

.msg-side-title:hover {
  color: var(--pink);
  background: var(--pink-bg-hover);
}

.quote-text {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--pink-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.75;
}

.empty-icon svg {
  width: 32px;
  height: 32px;
}

.empty-text {
  font-size: 14px;
  color: var(--text-dim);
  margin: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .mc-container {
    grid-template-columns: 1fr;
    padding: 16px;
    gap: 16px;
  }

  .mc-sidebar {
    border-radius: 12px;
  }

  .sidebar-nav {
    flex-direction: row;
    overflow-x: auto;
    padding: 8px;
    gap: 4px;
  }

  .nav-item {
    padding: 8px 14px;
    border-radius: 8px;
    white-space: nowrap;
    width: auto;
    gap: 6px;
    font-size: 13px;
  }

  .nav-item.active::before {
    display: none;
  }

  .nav-item.active {
    background: var(--pink-bg-hover);
    border-radius: 8px;
  }

  .nav-badge {
    min-width: 18px;
    height: 18px;
    font-size: 10px;
  }

  .message-item {
    padding: 12px 16px;
    gap: 10px;
  }

  .msg-target {
    max-width: 180px;
  }
}
</style>
