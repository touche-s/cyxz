<template>
  <main class="message-center">
    <div class="mc-container">
      <aside class="mc-sidebar">
        <div class="sidebar-brand">
          <div class="brand-icon"><Icon icon="ph:bell-ringing" /></div>
          <div>
            <p>互动通知</p>
            <h1>消息中心</h1>
          </div>
        </div>
        <nav class="sidebar-nav" aria-label="消息分类">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="nav-item"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <Icon :icon="tab.icon" class="nav-icon" />
            <span class="nav-label">{{ tab.label }}</span>
            <span class="nav-badge" v-if="tab.count > 0">{{ tab.count > 99 ? '99+' : tab.count }}</span>
          </button>
        </nav>
        <div class="sidebar-tip">
          <Icon icon="ph:heart-straight" />
          <span>你的每一次互动，都值得被看见。</span>
        </div>
      </aside>

      <section class="mc-main">
        <header class="message-hero">
          <div>
            <p class="hero-eyebrow">NOTIFICATIONS</p>
            <h2>{{ currentTabLabel }}</h2>
            <p class="hero-desc">查看大家与你的最新互动</p>
          </div>
          <div class="hero-actions">
            <span v-if="totalUnread > 0" class="unread-summary">{{ totalUnread }} 条未读</span>
            <button class="mark-all-btn" :disabled="totalUnread === 0" @click="handleMarkAllRead">
              <Icon icon="ph:check-double" />
              全部已读
            </button>
          </div>
        </header>

        <div class="message-content">
          <template v-for="(section, si) in displaySections" :key="si">
            <div class="section-header" v-if="section.title">
              <span class="section-title">{{ section.title }}</span>
              <span class="section-count" v-if="section.title === '最新'">{{ section.items.length }}</span>
            </div>
            <div class="message-list" v-if="section.items.length > 0">
              <article
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
                  <div v-else-if="msg.isSystem" class="msg-avatar system-avatar">
                    <Icon icon="ph:bell-simple-fill" />
                  </div>
                  <div class="msg-avatar" v-else @click.stop="goUser(msg.senderId)">
                    <img :src="avatarUrl(msg.senderAvatar)" alt="" />
                  </div>
                </div>
                <div class="msg-content">
                  <div class="msg-kind"><Icon :icon="typeIcon(msg.type)" /></div>
                  <div class="msg-main">
                    <div class="msg-top">
                      <template v-if="msg._merged">
                        <span class="msg-username merged-names" :title="msg.mergeNames">{{ msg.mergeNames }}</span>
                        <span class="msg-action" v-if="msg.actionText">{{ msg.actionText }}</span>
                      </template>
                      <template v-else>
                        <span class="msg-username" :class="{ 'system-username': msg.isSystem }" @click.stop="!msg.isSystem && goUser(msg.senderId)">{{ msg.senderName }}</span>
                        <span class="msg-action" v-if="msg.actionText">{{ msg.actionText }}</span>
                      </template>
                    </div>
                    <div class="msg-target" v-if="msg.targetTitle" @click.stop="goTarget(msg)">{{ msg.targetTitle }}</div>
                    <div class="msg-time">{{ msg.timeText }}</div>
                  </div>
                  <div class="msg-side" v-if="msg.quoteContent">
                    <div class="msg-quote"><span class="quote-text">{{ msg.quoteContent }}</span></div>
                  </div>
                  <span class="msg-dot" v-if="!msg.isRead"></span>
                </div>
              </article>
            </div>
          </template>

          <div class="empty-state" v-if="isEmpty">
            <div class="empty-icon"><Icon icon="ph:bell" /></div>
            <p class="empty-title">暂时没有{{ currentTabLabel }}消息</p>
            <p class="empty-text">有新的互动时，会第一时间出现在这里。</p>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useNavigate } from '@/composables/useNavigate'
import { useMessageStore } from '@/stores/message'
import { getNotifications, markAllRead, markRead, type NotificationVO } from '@/api/message'
import { avatarUrl } from '@/utils/avatar'
import { formatTime } from '@/utils/format'

const { open } = useNavigate()
const messageStore = useMessageStore()

const activeTab = ref('like')
const notifications = ref<NotificationVO[]>([])
const loading = ref(false)

// 类型到前端的映射
const typeConfig: Record<string, { label: string; frontType: string }> = {
  POST_LIKED: { label: '赞了你的帖子', frontType: 'like' },
  POST_COLLECTED: { label: '收藏了你的帖子', frontType: 'like' },
  POST_COMMENTED: { label: '评论了你的帖子', frontType: 'comment' },
  COMMENT_REPLIED: { label: '回复了你的评论', frontType: 'reply' },
  USER_FOLLOWED: { label: '关注了你', frontType: 'follow' },
  POST_APPROVED: { label: '审核通过', frontType: 'system' },
  POST_REJECTED: { label: '审核未通过', frontType: 'system' },
  POST_TAKEDOWN: { label: '你的帖子因违规被下架', frontType: 'system' },
  COMMENT_TAKEDOWN: { label: '你的评论因违规被删除', frontType: 'system' },
  REPORT_RESULT: { label: '举报处理结果', frontType: 'system' },
}

const commentUnread = computed(() => notifications.value.filter(m => (m.type === 'comment' || m.type === 'reply') && !m.isRead).length)
const likeUnread = computed(() => notifications.value.filter(m => m.type === 'like' && !m.isRead).length)
const followUnread = computed(() => notifications.value.filter(m => m.type === 'follow' && !m.isRead).length)
const systemUnread = computed(() => notifications.value.filter(m => m.type === 'system' && !m.isRead).length)
const totalUnread = computed(() => notifications.value.filter(m => !m.isRead).length)

const tabs = computed(() => [
  { key: 'like', label: '我收到的赞', icon: 'ph:heart-straight', count: likeUnread.value },
  { key: 'comment', label: '评论和回复', icon: 'ph:chat-circle-dots', count: commentUnread.value },
  { key: 'follow', label: '关注', icon: 'ph:user-plus', count: followUnread.value },
  { key: 'system', label: '系统通知', icon: 'ph:bell', count: systemUnread.value },
])

const currentTabLabel = computed(() => {
  const map: Record<string, string> = {
    like: '我收到的赞', comment: '评论和回复', follow: '关注', system: '系统通知',
  }
  return map[activeTab.value] || '消息'
})

/** 点赞 tab 需要分区展示（最新 / 累计），系统通知不合并 */
const mergeTab = computed(() => activeTab.value === 'like')

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

function mapNotification(n: NotificationVO): NotificationVO & { timeText: string; isSystem: boolean } {
  const cfg = typeConfig[n.type] || { label: '与你互动', frontType: 'system' }
  const isSystem = cfg.frontType === 'system'
  return {
    ...n,
    // 优先使用后端下发的动作文案（含审核拒绝原因等动态内容），无则回退类型映射
    actionText: n.actionText || cfg.label,
    senderName: isSystem ? '系统通知' : (n.senderName || '用户'),
    senderAvatar: isSystem ? '' : (n.senderAvatar || ''),
    isRead: n.isRead,
    timeText: formatTime(n.createTime),
    type: cfg.frontType as any,
    isSystem,
  } as any
}

function typeIcon(type: string): string {
  const icons: Record<string, string> = {
    like: 'ph:heart-straight-fill',
    comment: 'ph:chat-circle-dots-fill',
    reply: 'ph:arrow-bend-up-left',
    collect: 'ph:star-fill',
    follow: 'ph:user-plus',
  }
  return icons[type] || 'ph:bell'
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
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 24px 24px 0;
  display: grid;
  grid-template-columns: 196px 1fr;
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
  border: 1px solid var(--border-light);
  box-shadow: 0 2px 12px rgba(180, 132, 255, 0.05);
  height: fit-content;
  overflow: hidden;
}

.sidebar-header {
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--border-light);
}

.sidebar-header h2 {
  font-size: 16px;
  font-weight: 800;
  color: var(--text);
  margin: 0;
}

.sidebar-nav {
  padding: 6px 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-dim);
  transition: all 0.18s ease;
  width: 100%;
  text-align: left;
  position: relative;
}

.nav-item:hover {
  color: var(--pink);
  background: rgba(255, 107, 157, 0.05);
}

.nav-item.active {
  color: var(--pink);
  font-weight: 700;
  background: linear-gradient(90deg, rgba(255, 107, 157, 0.06), transparent);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 5px;
  bottom: 5px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, var(--pink), var(--purple));
}

.nav-label {
  line-height: 1.2;
}

.nav-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  font-size: 10px;
  font-weight: 700;
  color: var(--white);
  background: var(--gradient-brand);
  line-height: 1;
}

/* ===== 右侧内容区 ===== */
.mc-main {
  width: 660px;
  max-width: 100%;
  background: var(--card);
  border-radius: 16px;
  border: 1px solid var(--border-light);
  box-shadow: 0 2px 12px rgba(180, 132, 255, 0.05);
  overflow: hidden;
  margin: 0 auto;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid var(--border-light);
}

.main-header h3 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

/* 分区标题 */
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px 2px;
}

.section-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--pink);
  letter-spacing: 0.3px;
}

.section-count {
  font-size: 10px;
  font-weight: 700;
  color: var(--white);
  background: var(--gradient-brand);
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.mark-all-btn {
  padding: 5px 14px;
  border-radius: 7px;
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--pink);
  font-size: 12px;
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
  padding: 2px 0;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 20px;
  transition: background 0.15s ease;
  cursor: pointer;
}

.message-item:hover {
  background: rgba(255, 107, 157, 0.04);
}

.message-item + .message-item {
  border-top: 1px solid rgba(255, 107, 157, 0.05);
}

.message-item.unread {
  background: rgba(255, 107, 157, 0.04);
}

.msg-avatar-col {
  width: 46px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  transition: opacity 0.15s ease;
  border: 1px solid rgba(255, 107, 157, 0.12);
  background: var(--card);
}

.msg-avatar:hover {
  opacity: 0.84;
}

.msg-avatar.system-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.12), rgba(180, 132, 255, 0.16));
  color: var(--pink);
  cursor: default;
}

.msg-avatar.system-avatar:hover {
  opacity: 1;
}

.msg-avatar.system-avatar :deep(svg) {
  width: 18px;
  height: 18px;
}

.msg-avatar:not(.merged-avatar) img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 堆叠头像 */
.msg-avatar.merged-avatar {
  width: 46px;
  height: 36px;
  border: none;
  border-radius: 0;
  overflow: visible;
  cursor: default;
  background: transparent;
}

.avatar-stack {
  position: relative;
  width: 100%;
  height: 28px;
}

.stacked-avatar {
  position: absolute;
  top: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid var(--card);
  object-fit: cover;
  display: block;
  background: var(--card);
}

.merged-names {
  max-width: 280px;
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
  gap: 14px;
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
  line-height: 1.5;
}

.msg-username {
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
  cursor: pointer;
  transition: color 0.15s;
}

.msg-username:hover {
  color: var(--pink);
}

.msg-username.system-username {
  cursor: default;
}

.msg-username.system-username:hover {
  color: var(--text);
}

.msg-action {
  font-size: 13px;
  color: var(--text-dim);
}

.msg-target {
  font-size: 13px;
  color: var(--text-dim);
  cursor: pointer;
  transition: color 0.15s;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 240px;
}

.msg-target:hover {
  color: var(--pink);
}

.msg-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--gradient-brand);
  flex-shrink: 0;
  margin-left: 4px;
}

.msg-meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.msg-time {
  font-size: 12px;
  color: var(--text-dim);
}

.msg-side {
  width: 100px;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
}

.msg-quote,
.msg-side-title {
  width: 100px;
  padding: 6px 8px;
  background: var(--pink-bg);
  border-radius: 7px;
  font-size: 12px;
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

<style scoped>
.message-center {
  min-height: 100vh;
  padding: 98px 24px 56px;
  background: radial-gradient(circle at 8% 12%, rgba(255, 139, 194, 0.15), transparent 25%), radial-gradient(circle at 92% 88%, rgba(192, 132, 252, 0.13), transparent 25%), var(--bg);
}

.mc-container {
  width: min(1120px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.mc-sidebar,
.mc-main {
  background: var(--card);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow);
}

.mc-sidebar { position: sticky; top: 92px; padding: 18px 12px 14px; border-radius: 22px; }
.sidebar-brand { display: flex; align-items: center; gap: 12px; padding: 4px 8px 20px; }
.brand-icon { display: grid; width: 40px; height: 40px; place-items: center; border-radius: 14px; color: var(--white); background: var(--gradient-brand); box-shadow: 0 8px 18px rgba(255, 107, 157, 0.2); }
.brand-icon svg { width: 21px; height: 21px; }
.sidebar-brand p,
.hero-eyebrow { margin: 0; color: var(--text-dim); font-size: 10px; font-weight: 800; letter-spacing: 0.12em; text-transform: uppercase; }
.sidebar-brand h1 { margin: 3px 0 0; color: var(--text); font-size: 18px; font-weight: 800; }
.sidebar-nav { display: flex; flex-direction: column; gap: 4px; padding: 0; }
.nav-item { display: flex; align-items: center; gap: 10px; width: 100%; padding: 11px 12px; border: 0; border-radius: 12px; color: var(--text-dim); background: transparent; font: inherit; font-size: 13px; font-weight: 600; text-align: left; cursor: pointer; transition: color 0.2s ease, background 0.2s ease, transform 0.2s ease; }
.nav-item:hover { color: var(--pink); background: var(--pink-bg); }
.nav-item.active { color: var(--pink); background: linear-gradient(135deg, var(--pink-bg-hover), var(--purple-bg)); font-weight: 700; }
.nav-item.active::before { display: none; }
.nav-icon { width: 18px; height: 18px; flex-shrink: 0; }
.nav-label { flex: 1; }
.nav-badge,
.section-count { display: inline-flex; align-items: center; justify-content: center; min-width: 20px; height: 20px; padding: 0 6px; border-radius: 999px; color: var(--white); background: var(--gradient-brand); font-size: 10px; font-weight: 800; }
.sidebar-tip { display: flex; gap: 7px; margin: 22px 4px 2px; padding: 12px; border-radius: 14px; color: var(--text-dim); background: var(--pink-bg); font-size: 11px; line-height: 1.6; }
.sidebar-tip svg { width: 15px; height: 15px; color: var(--pink); flex-shrink: 0; margin-top: 1px; }

.mc-main { width: auto; max-width: none; overflow: hidden; margin: 0; border-radius: 24px; }
.message-hero { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 28px 30px 24px; border-bottom: 1px solid var(--border-light); background: linear-gradient(120deg, var(--pink-bg), transparent 60%, var(--purple-bg)); }
.message-hero h2 { margin: 5px 0 4px; color: var(--text); font-size: 23px; font-weight: 800; }
.hero-desc { margin: 0; color: var(--text-dim); font-size: 13px; }
.hero-actions { display: flex; align-items: center; gap: 12px; }
.unread-summary { color: var(--pink); font-size: 12px; font-weight: 700; white-space: nowrap; }
.mark-all-btn { display: inline-flex; align-items: center; gap: 6px; padding: 9px 13px; border: 1px solid var(--border); border-radius: 10px; color: var(--pink); background: var(--card); font: inherit; font-size: 12px; font-weight: 700; cursor: pointer; transition: color 0.2s ease, background 0.2s ease, border-color 0.2s ease; }
.mark-all-btn svg { width: 16px; height: 16px; }
.mark-all-btn:hover:not(:disabled) { background: var(--pink-bg); border-color: var(--pink); }
.mark-all-btn:disabled { cursor: default; opacity: 0.48; }
.message-content { padding: 12px; }
.section-header { display: flex; align-items: center; gap: 8px; padding: 16px 8px 8px; }
.section-title { color: var(--text-dim); font-size: 12px; font-weight: 800; }
.message-list { display: flex; flex-direction: column; gap: 3px; padding: 0; }
.message-item { display: flex; align-items: flex-start; gap: 12px; padding: 14px; border: 1px solid transparent; border-radius: 16px; cursor: pointer; transition: background 0.2s ease, border-color 0.2s ease, transform 0.2s ease; }
.message-item + .message-item { border-top: 1px solid transparent; }
.message-item:hover { border-color: var(--border-light); background: var(--pink-bg); transform: translateX(2px); }
.message-item.unread { border-color: var(--border-light); background: linear-gradient(135deg, var(--pink-bg), rgba(255, 255, 255, 0)); }
.msg-avatar-col { width: 42px; flex-shrink: 0; }
.msg-avatar { width: 40px; height: 40px; overflow: hidden; border: 2px solid var(--card); border-radius: 14px; background: var(--gradient-card); box-shadow: 0 3px 10px rgba(255, 107, 157, 0.1); }
.msg-avatar:not(.merged-avatar) img { width: 100%; height: 100%; object-fit: cover; display: block; }
.msg-avatar.merged-avatar { width: 46px; height: 40px; overflow: visible; border: 0; border-radius: 0; background: transparent; box-shadow: none; }
.avatar-stack { position: relative; width: 100%; height: 32px; }
.stacked-avatar { position: absolute; top: 2px; width: 30px; height: 30px; border: 2px solid var(--card); border-radius: 50%; object-fit: cover; background: var(--card); }
.msg-content { display: flex; flex: 1; min-width: 0; align-items: flex-start; gap: 10px; }
.msg-kind { display: grid; width: 24px; height: 24px; flex-shrink: 0; place-items: center; border-radius: 8px; color: var(--pink); background: var(--pink-bg-hover); }
.msg-kind svg { width: 14px; height: 14px; }
.msg-main { flex: 1; min-width: 0; padding-top: 1px; }
.msg-top { display: flex; align-items: baseline; flex-wrap: wrap; gap: 5px; line-height: 1.45; }
.msg-username { color: var(--text); font-size: 14px; font-weight: 750; transition: color 0.2s ease; }
.msg-username:hover { color: var(--pink); }
.merged-names { max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.msg-action { color: var(--text-dim); font-size: 13px; }
.msg-target { margin-top: 4px; max-width: none; overflow: hidden; color: var(--text); font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; transition: color 0.2s ease; }
.msg-target:hover { color: var(--pink); }
.msg-time { margin-top: 5px; color: var(--text-dim); font-size: 11px; }
.msg-side { width: 116px; flex-shrink: 0; display: block; }
.msg-quote { width: auto; padding: 7px 8px; border-radius: 9px; color: var(--text-dim); background: var(--bg-soft); font-size: 11px; line-height: 1.5; }
.msg-dot { width: 7px; height: 7px; flex-shrink: 0; margin: 8px 2px 0 0; border-radius: 50%; background: var(--pink); box-shadow: 0 0 0 4px var(--pink-bg); }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 390px; padding: 48px 20px; text-align: center; }
.empty-icon { display: grid; width: 76px; height: 76px; place-items: center; border-radius: 26px; color: var(--pink); background: linear-gradient(135deg, var(--pink-bg-hover), var(--purple-bg)); }
.empty-icon svg { width: 35px; height: 35px; }
.empty-title { margin: 18px 0 6px; color: var(--text); font-size: 15px; font-weight: 700; }
.empty-text { margin: 0; color: var(--text-dim); font-size: 13px; }

@media (max-width: 768px) {
  .message-center { padding: 84px 14px 32px; }
  .mc-container { grid-template-columns: 1fr; gap: 14px; }
  .mc-sidebar { position: static; padding: 14px; }
  .sidebar-brand { padding-bottom: 12px; }
  .sidebar-tip { display: none; }
  .sidebar-nav { flex-direction: row; overflow-x: auto; padding-bottom: 2px; }
  .nav-item { width: auto; min-width: max-content; padding: 9px 10px; }
  .mc-main { border-radius: 18px; }
  .message-hero { padding: 22px 20px; }
  .message-hero h2 { font-size: 20px; }
  .hero-actions { gap: 8px; }
  .unread-summary { display: none; }
  .message-content { padding: 8px; }
  .message-item { padding: 12px 10px; }
  .msg-side { display: none; }
  .merged-names { max-width: 180px; }
}
</style>
