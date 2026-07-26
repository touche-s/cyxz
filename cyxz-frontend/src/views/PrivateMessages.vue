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
            <input type="text" class="pm-search-input" placeholder="搜索对话..." />
          </div>
        </div>
        <div class="pm-side-label">最近消息</div>
        <div class="pm-conv-list">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="pm-conv-item"
            :class="{ active: selectedId === conv.id }"
            @click="selectConv(conv.id)"
          >
            <div class="pm-conv-avatar" :style="{ background: conv.avatarGrad }">
              {{ conv.avatarChar }}
            </div>
            <div class="pm-conv-main">
              <div class="pm-conv-top">
                <span class="pm-conv-name">{{ conv.name }}</span>
                <span class="pm-conv-time">{{ conv.time }}</span>
              </div>
              <div class="pm-conv-bottom">
                <span class="pm-conv-preview">{{ conv.lastMessage }}</span>
                <span v-if="conv.unread > 0" class="pm-conv-badge">{{ conv.unread > 99 ? '99+' : conv.unread }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="pm-chat" v-show="!isMobile || showChat">
        <template v-if="selectedConv">
          <div class="pm-chat-hd">
            <button v-if="isMobile" class="pm-chat-back" @click="showChat = false">
              <Icon icon="ph:arrow-left" />
            </button>
            <div class="pm-chat-avatar-sm" :style="{ background: selectedConv.avatarGrad }">
              {{ selectedConv.avatarChar }}
            </div>
            <div class="pm-chat-hd-info">
              <span class="pm-chat-hd-name">{{ selectedConv.name }}</span>
              <span class="pm-chat-hd-status">
                <span class="pm-status-dot"></span>
                {{ selectedConv.status }}
              </span>
            </div>
            <div class="pm-chat-hd-actions">
              <button class="pm-hd-btn"><Icon icon="ph:magnifying-glass" /></button>
              <button class="pm-hd-btn"><Icon icon="ph:dots-three-outline-vertical" /></button>
            </div>
          </div>

          <div class="pm-chat-body" ref="chatBody">
            <div class="pm-time-sep"><span>{{ selectedConv.timeLabel }}</span></div>
            <div
              v-for="msg in selectedConv.messages"
              :key="msg.id"
              class="pm-msg"
              :class="{ 'pm-msg--self': msg.sender === 'self' }"
            >
              <div class="pm-msg-bubble">
                {{ msg.content }}
              </div>
              <span class="pm-msg-time">{{ msg.time }}</span>
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
                @keydown.enter="sendMessage"
              />
              <button
                class="pm-ft-send"
                :class="{ disabled: !newMessage.trim() }"
                @click="sendMessage"
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { Icon } from '@iconify/vue'

interface Message {
  id: number
  sender: 'self' | 'other'
  content: string
  time: string
}

interface Conversation {
  id: number
  name: string
  avatarChar: string
  avatarGrad: string
  lastMessage: string
  time: string
  timeLabel: string
  unread: number
  status: string
  messages: Message[]
}

const chatBody = ref<HTMLElement>()

const conversations = ref<Conversation[]>([
  {
    id: 1,
    name: '星野爱',
    avatarChar: '星',
    avatarGrad: 'linear-gradient(135deg, #f9a8d4, #c4b5fd)',
    lastMessage: '那个新番的作画真的太绝了！逐帧都是壁纸级别',
    time: '2分钟前',
    timeLabel: '今天 14:32',
    unread: 2,
    status: '正在浏览圈子',
    messages: [
      { id: 1, sender: 'other', content: '你看这季度的《星屑幻想》了吗？', time: '14:20' },
      { id: 2, sender: 'self', content: '看了看了！第一集就被震撼到了，那个星空场景美到窒息 😭', time: '14:22' },
      { id: 3, sender: 'other', content: '对吧对吧！而且 BGM 也超神，我已经循环 OST 了', time: '14:24' },
      { id: 4, sender: 'self', content: '制作组是真的下了功夫，每个角色的微表情都画得好细腻', time: '14:26' },
      { id: 5, sender: 'other', content: '下集预告里那个新角色也好期待！好像是千夏配的音', time: '14:28' },
      { id: 6, sender: 'self', content: '啊真的吗？！千夏的声音确实很适合这种温柔系角色', time: '14:30' },
      { id: 7, sender: 'other', content: '那个新番的作画真的太绝了！逐帧都是壁纸级别', time: '14:32' },
    ],
  },
  {
    id: 2,
    name: '千夏',
    avatarChar: '千',
    avatarGrad: 'linear-gradient(135deg, #a5f3fc, #c4b5fd)',
    lastMessage: '我也超喜欢这个角色的，尤其是她那种不服输的性格',
    time: '12分钟前',
    timeLabel: '今天 14:18',
    unread: 1,
    status: '在线',
    messages: [
      { id: 1, sender: 'other', content: '你推的那个角色我也开始喜欢了！', time: '14:10' },
      { id: 2, sender: 'self', content: '哈哈我就说吧，她塑造得太有层次感了', time: '14:12' },
      { id: 3, sender: 'other', content: '我也超喜欢这个角色的，尤其是她那种不服输的性格', time: '14:18' },
    ],
  },
  {
    id: 3,
    name: '阿澈',
    avatarChar: '澈',
    avatarGrad: 'linear-gradient(135deg, #fde68a, #f9a8d4)',
    lastMessage: '周末一起去漫展吗？听说有超多限定周边',
    time: '1小时前',
    timeLabel: '今天 13:45',
    unread: 0,
    status: '离线',
    messages: [
      { id: 1, sender: 'self', content: '最近有啥好看的番推荐吗？', time: '13:30' },
      { id: 2, sender: 'other', content: '强推《幻境旅人》！剧情反转超精彩，我一天刷完了', time: '13:35' },
      { id: 3, sender: 'self', content: '听起来不错，正好周末没事，安排上', time: '13:40' },
      { id: 4, sender: 'other', content: '周末一起去漫展吗？听说有超多限定周边', time: '13:45' },
    ],
  },
  {
    id: 4,
    name: '小原好美推',
    avatarChar: '推',
    avatarGrad: 'linear-gradient(135deg, #f9a8d4, #fda4af)',
    lastMessage: '谢谢你推荐的那部番！我已经看到第五集了',
    time: '3小时前',
    timeLabel: '今天 11:20',
    unread: 3,
    status: '正在写同人文',
    messages: [
      { id: 1, sender: 'other', content: '你有没有那种看完之后久久走不出来的番？', time: '11:00' },
      { id: 2, sender: 'self', content: '必须有！《星屑幻想》前身的那部《银河少年》就是，结尾那段我哭了好久', time: '11:05' },
      { id: 3, sender: 'other', content: '啊啊啊我知道那部！导演的叙事手法太厉害了，当年拿奖拿了一堆', time: '11:10' },
      { id: 4, sender: 'self', content: '对对对，而且配乐是梶浦由记做的，每一首都值得单曲循环', time: '11:15' },
      { id: 5, sender: 'other', content: '谢谢你推荐的那部番！我已经看到第五集了', time: '11:20' },
    ],
  },
  {
    id: 5,
    name: '次元速报',
    avatarChar: '速',
    avatarGrad: 'linear-gradient(135deg, #c084fc, #a78bfa)',
    lastMessage: '本周热门话题：「新番推荐」大家最期待哪部？',
    time: '昨天',
    timeLabel: '昨天 20:30',
    unread: 0,
    status: '官方账号',
    messages: [
      { id: 1, sender: 'other', content: '🎉 本周热门圈子已更新！快来看看有没有你喜欢的作品上榜', time: '20:00' },
      { id: 2, sender: 'other', content: '🏆 本周 Top 3：1. 星屑幻想 2. 幻境旅人 3. 魔法少女养成记', time: '20:05' },
      { id: 3, sender: 'other', content: '本周热门话题：「新番推荐」大家最期待哪部？', time: '20:30' },
    ],
  },
])

const selectedId = ref(1)
const newMessage = ref('')
const isMobile = ref(false)
const showChat = ref(false)

const selectedConv = computed(() => conversations.value.find(c => c.id === selectedId.value))

function selectConv(id: number) {
  selectedId.value = id
  if (isMobile.value) showChat.value = true
  nextTick(() => scrollToBottom())
}

function sendMessage() {
  const text = newMessage.value.trim()
  if (!text || !selectedConv.value) return
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const timeStr = `${h}:${m}`
  selectedConv.value.messages.push({
    id: Date.now(),
    sender: 'self',
    content: text,
    time: timeStr,
  })
  selectedConv.value.lastMessage = text
  selectedConv.value.time = '刚刚'
  newMessage.value = ''
  nextTick(() => scrollToBottom())
}

function scrollToBottom() {
  if (chatBody.value) {
    chatBody.value.scrollTop = chatBody.value.scrollHeight
  }
}

onMounted(() => {
  const mq = window.matchMedia('(max-width: 768px)')
  isMobile.value = mq.matches
  mq.addEventListener('change', (e) => { isMobile.value = e.matches })
  nextTick(() => scrollToBottom())
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
  background: linear-gradient(135deg, #ff6b9d, #c084fc);
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
  background: linear-gradient(135deg, var(--pink), var(--purple));
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
