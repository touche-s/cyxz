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
        <!-- 右侧消息列表 -->
        <main class="mc-main">
        <div class="main-header">
          <h3>{{ currentTabLabel }}</h3>
          <button class="mark-all-btn" @click="markAllRead">全部已读</button>
        </div>

        <div class="message-list" v-if="filteredMessages.length > 0">
          <div
            v-for="msg in filteredMessages"
            :key="msg.id"
            class="message-item"
            :class="{ unread: !msg.read }"
          >
            <div class="msg-avatar" :class="{ system: msg.type === 'system' }" @click="msg.type !== 'system' && goUser(msg.userId)">
              <template v-if="msg.type === 'system'">
                <i-ph-bell class="msg-type-icon pink-icon" />
              </template>
              <template v-else>
                <img v-if="msg.userAvatar" :src="msg.userAvatar" alt="" />
                <div v-else class="avatar-placeholder">{{ (msg.userName || '?').charAt(0) }}</div>
              </template>
            </div>
            <div class="msg-body">
              <div class="msg-top">
                <span class="msg-username" :class="{ system: msg.type === 'system' }" @click="msg.type !== 'system' && goUser(msg.userId)">{{ msg.userName }}</span>
                <span class="msg-action" v-if="msg.actionText">{{ msg.actionText }}</span>
                <span class="msg-target" v-if="msg.targetTitle" @click="goTarget(msg)">{{ msg.targetTitle }}</span>
                <span class="msg-dot" v-if="!msg.read"></span>
              </div>
              <div class="msg-quote" v-if="msg.quoteContent">
                <span class="quote-text">"{{ msg.quoteContent }}"</span>
              </div>
              <div class="msg-time">{{ msg.timeText }}</div>
            </div>
          </div>
        </div>

        <div class="empty-state" v-else>
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
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { useNavigate } from '@/composables/useNavigate'

const { open } = useNavigate()

interface MessageItem {
  id: number
  type: 'like' | 'comment' | 'reply' | 'follow' | 'collect' | 'chat' | 'mention' | 'system'
  userId: number
  userName: string
  userAvatar: string
  actionText: string
  targetTitle?: string
  targetId?: number
  targetType?: string
  quoteContent?: string
  read: boolean
  timeText: string
}

const activeTab = ref('all')

const tabs = computed(() => [
  { key: 'all', label: '全部消息', count: messages.value.filter(m => !m.read).length },
  { key: 'chat', label: '私信', count: messages.value.filter(m => m.type === 'chat' && !m.read).length },
  { key: 'mention', label: '@我的', count: messages.value.filter(m => m.type === 'mention' && !m.read).length },
  { key: 'comment', label: '评论和回复', count: messages.value.filter(m => (m.type === 'comment' || m.type === 'reply') && !m.read).length },
  { key: 'like', label: '我收到的赞', count: messages.value.filter(m => m.type === 'like' && !m.read).length },
  { key: 'system', label: '系统通知', count: messages.value.filter(m => m.type === 'system' && !m.read).length },
])

const currentTabLabel = computed(() => {
  const map: Record<string, string> = {
    all: '全部消息',
    chat: '私信',
    mention: '@我的',
    comment: '评论和回复',
    like: '我收到的赞',
    system: '系统通知',
  }
  return map[activeTab.value] || '消息'
})

const filteredMessages = computed(() => {
  if (activeTab.value === 'all') return messages.value
  if (activeTab.value === 'comment') return messages.value.filter(m => m.type === 'comment' || m.type === 'reply')
  return messages.value.filter(m => m.type === activeTab.value)
})

const messages = ref<MessageItem[]>([
  {
    id: 1,
    type: 'chat',
    userId: 101,
    userName: '樱小路露娜',
    userAvatar: '',
    actionText: '发送了私信',
    quoteContent: '太太你好~想问一下最近接稿吗，很喜欢你的画风！',
    read: false,
    timeText: '3分钟前',
  },
  {
    id: 2,
    type: 'chat',
    userId: 105,
    userName: '七海灯子',
    userAvatar: '',
    actionText: '发送了私信',
    quoteContent: '看到了！笔刷推荐你试试DAUB的Blender系列',
    read: false,
    timeText: '15分钟前',
  },
  {
    id: 3,
    type: 'mention',
    userId: 102,
    userName: '夜空下的星尘',
    userAvatar: '',
    actionText: '在评论中 @了你',
    targetTitle: '赛马娘同人图楼',
    targetId: 3,
    targetType: 'post',
    quoteContent: '@次元小站 这张有没有更高清的版本呀！',
    read: false,
    timeText: '18分钟前',
  },
  {
    id: 4,
    type: 'comment',
    userId: 103,
    userName: '喵喵拳',
    userAvatar: '',
    actionText: '评论了你的帖子',
    targetTitle: '最近摸鱼画的水彩风插画合集',
    targetId: 2,
    targetType: 'post',
    quoteContent: '画风好好看！请问大大用的什么笔刷啊？',
    read: true,
    timeText: '42分钟前',
  },
  {
    id: 5,
    type: 'reply',
    userId: 104,
    userName: '白夜凛音',
    userAvatar: '',
    actionText: '回复了你的评论',
    targetTitle: '最近摸鱼画的水彩风插画合集',
    targetId: 2,
    targetType: 'post',
    quoteContent: '同问！求笔刷推荐 ~',
    read: true,
    timeText: '1小时前',
  },
  {
    id: 6,
    type: 'like',
    userId: 101,
    userName: '樱小路露娜',
    userAvatar: '',
    actionText: '赞了你的帖子',
    targetTitle: '【板绘教程】从零开始的SAI上色技巧分享',
    targetId: 1,
    targetType: 'post',
    read: false,
    timeText: '2小时前',
  },
  {
    id: 7,
    type: 'like',
    userId: 106,
    userName: '夜刀神十香',
    userAvatar: '',
    actionText: '赞了你的评论',
    targetTitle: '赛马娘同人图楼',
    targetId: 3,
    targetType: 'post',
    read: true,
    timeText: '5小时前',
  },
  {
    id: 8,
    type: 'like',
    userId: 107,
    userName: '时崎狂三',
    userAvatar: '',
    actionText: '赞了你的帖子',
    targetTitle: '【板绘教程】从零开始的SAI上色技巧分享',
    targetId: 1,
    targetType: 'post',
    read: true,
    timeText: '昨天 23:14',
  },
  {
    id: 9,
    type: 'like',
    userId: 108,
    userName: '霞之丘诗羽',
    userAvatar: '',
    actionText: '赞了你的帖子',
    targetTitle: '赛马娘同人图楼',
    targetId: 3,
    targetType: 'post',
    read: true,
    timeText: '昨天 20:30',
  },
  {
    id: 10,
    type: 'system',
    userId: 0,
    userName: '系统',
    userAvatar: '',
    actionText: '',
    quoteContent: '恭喜你，作品【板绘教程】进入今日热门推荐！',
    read: true,
    timeText: '昨天 18:22',
  },
  {
    id: 11,
    type: 'system',
    userId: 0,
    userName: '系统',
    userAvatar: '',
    actionText: '',
    quoteContent: '你的粉丝数突破100啦，继续加油！',
    read: true,
    timeText: '前天 15:40',
  },
  {
    id: 12,
    type: 'comment',
    userId: 109,
    userName: '五更琉璃',
    userAvatar: '',
    actionText: '评论了你的帖子',
    targetTitle: '最近摸鱼画的水彩风插画合集',
    targetId: 2,
    targetType: 'post',
    quoteContent: '颜色搭配很舒服，已关注！',
    read: true,
    timeText: '前天 11:08',
  },
  {
    id: 13,
    type: 'follow',
    userId: 110,
    userName: '小鸟游六花',
    userAvatar: '',
    actionText: '关注了你',
    read: true,
    timeText: '前天 10:22',
  },
  {
    id: 14,
    type: 'collect',
    userId: 111,
    userName: '和泉纱雾',
    userAvatar: '',
    actionText: '收藏了你的帖子',
    targetTitle: '最近摸鱼画的水彩风插画合集',
    targetId: 2,
    targetType: 'post',
    read: true,
    timeText: '3天前',
  },
])

function markAllRead() {
  messages.value.forEach(m => { m.read = true })
}

function goToUser(userId: number) {
  open(`/user/${userId}`)
}

function goTarget(msg: MessageItem) {
  if (msg.targetType === 'post' && msg.targetId) {
    open(`/post/${msg.targetId}`)
  }
}
</script>

<style scoped>
.message-center {
  min-height: calc(100vh - 78px);
  padding-top: 78px;
  background: linear-gradient(180deg, #fdf4f9 0%, #faf5ff 100%);
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
  padding: 20px 28px;
  border-bottom: 1px solid var(--border-light);
}

.main-header h3 {
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
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
  padding: 8px 0;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 28px;
  transition: background 0.15s ease;
  cursor: pointer;
}

.message-item:hover {
  background: rgba(255, 107, 157, 0.03);
}

.message-item + .message-item {
  border-top: 1px solid rgba(255, 107, 157, 0.05);
}

.message-item.unread {
  background: rgba(255, 107, 157, 0.02);
}

.msg-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  transition: opacity 0.15s;
  border: 2px solid rgba(255, 107, 157, 0.12);
}

.msg-avatar.system {
  cursor: default;
  border-color: rgba(180, 132, 255, 0.15);
  background: var(--purple-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--purple);
}
.msg-type-icon {
  width: 20px;
  height: 20px;
}

.msg-avatar.system:hover {
  opacity: 1;
}

.msg-avatar:hover {
  opacity: 0.8;
}

.msg-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  font-weight: 700;
}

.msg-body {
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
  white-space: nowrap;
}

.msg-username.system {
  color: var(--purple);
  cursor: default;
}

.msg-username.system:hover {
  color: var(--purple);
}

.msg-username:hover {
  color: var(--pink);
}

.msg-action {
  font-size: 13px;
  color: var(--text-dim);
  white-space: nowrap;
}

.msg-target {
  font-size: 13px;
  color: var(--pink);
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 260px;
  transition: opacity 0.15s;
}

.msg-target:hover {
  opacity: 0.7;
}

.msg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  flex-shrink: 0;
  margin-left: 4px;
}

.msg-quote {
  margin-top: 8px;
  padding: 10px 14px;
  background: var(--pink-bg);
  border-radius: 8px;
  border-left: 3px solid var(--border);
}

.quote-text {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.msg-time {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-dim);
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
    padding: 14px 16px;
    gap: 12px;
  }

  .msg-avatar {
    width: 38px;
    height: 38px;
  }

  .msg-target {
    max-width: 180px;
  }
}
</style>
