<template>
  <div class="creator-home">
    <div class="home-hero">
      <div class="hero-main">
        <div class="hero-greeting">
          <span class="greeting-text">{{ greetingText }}</span>
          <h1 class="greeting-guide">继续创作，让灵感发光</h1>
        </div>
        <button class="hero-publish-btn" @click="$emit('goCreate')">
          <Icon icon="ph:pencil-simple" class="btn-icon" />
          <span>发布新作品</span>
        </button>
      </div>
    </div>

    <div class="dashboard-layout">
      <div class="stats-panel">
        <div class="panel-header">
          <div>
            <h3 class="section-title">数据概览</h3>
            <p class="section-desc">快速查看当前账号的整体表现</p>
          </div>
          <span class="panel-link" @click="goData">查看完整趋势 →</span>
        </div>
        <div class="stats-grid">
          <StatCard icon-class="works-icon" :icon="iconWorks" :value="dataStats?.totalPosts ?? 0" label="总作品" />
          <StatCard icon-class="views-icon views-icon--primary" :icon="iconEye" :value="dataStats?.totalViews ?? 0" label="总浏览" />
          <StatCard icon-class="likes-icon" :icon="iconLike" :value="dataStats?.totalLikes ?? 0" label="总点赞" />
          <StatCard icon-class="collections-icon" :icon="iconFavorite" :value="dataStats?.totalCollections ?? 0" label="总收藏" />
          <StatCard icon-class="fans-icon" :icon="iconFans" :value="followerCount" label="粉丝数" />
          <StatCard icon-class="comments-icon" :icon="iconComment" :value="commentsTotal" label="评论数" />
        </div>
      </div>

      <div class="card-panel pending-panel">
        <div class="panel-header panel-header--compact">
          <div>
            <h3 class="section-title">待处理</h3>
            <p class="section-desc">优先处理需要你关注的事项</p>
          </div>
        </div>
        <div class="pending-grid">
          <div class="pending-card pending-draft" @click="goDraft" :class="{ 'has-count': draftCount > 0 }">
            <div class="pending-icon-wrap">
              <Icon icon="ph:note-pencil" class="pending-icon" />
            </div>
            <div class="pending-content">
              <div class="pending-top">
                <span class="pending-label">草稿</span>
                <span class="pending-num">{{ draftCount }}</span>
              </div>
              <span class="pending-sub">{{ draftCount > 0 ? '完善后即可发布' : '当前没有待完善草稿' }}</span>
            </div>
          </div>

          <div class="pending-card pending-card--disabled">
            <div class="pending-icon-wrap">
              <Icon icon="ph:clipboard-text" class="pending-icon" />
            </div>
            <div class="pending-content">
              <div class="pending-top">
                <span class="pending-label">待审核</span>
                <span class="pending-num">—</span>
              </div>
              <span class="pending-sub">功能建设中</span>
            </div>
          </div>

          <div class="pending-card pending-card--disabled">
            <div class="pending-icon-wrap">
              <Icon icon="ph:x-circle" class="pending-icon" />
            </div>
            <div class="pending-content">
              <div class="pending-top">
                <span class="pending-label">驳回</span>
                <span class="pending-num">—</span>
              </div>
              <span class="pending-sub">功能建设中</span>
            </div>
          </div>
        </div>
      </div>

      <div class="dashboard-side">
        <div class="card-panel">
          <div class="panel-header panel-header--compact">
            <div>
              <h3 class="section-title">今日数据</h3>
              <p class="section-desc">今日新增互动一览</p>
            </div>
          </div>
          <div class="today-list">
            <div class="today-row">
              <div class="today-left">
                <Icon icon="ph:heart" class="today-dot likes-dot" />
                <span class="today-row-label">新增点赞</span>
              </div>
              <span class="today-row-val">{{ todayStats.todayLikes }}</span>
            </div>
            <div class="today-row">
              <div class="today-left">
                <Icon icon="ph:star" class="today-dot collects-dot" />
                <span class="today-row-label">新增收藏</span>
              </div>
              <span class="today-row-val">{{ todayStats.todayCollections }}</span>
            </div>
            <div class="today-row">
              <div class="today-left">
                <Icon icon="ph:chat-circle-dots" class="today-dot comments-dot" />
                <span class="today-row-label">新增评论</span>
              </div>
              <span class="today-row-val">{{ todayStats.todayComments }}</span>
            </div>
          </div>
        </div>

        <div class="card-panel fans-panel" @click="goFans">
          <div class="panel-header panel-header--compact">
            <div>
              <h3 class="section-title">粉丝概览</h3>
              <p class="section-desc">查看粉丝变化与互动情况</p>
            </div>
            <Icon icon="ph:caret-right" class="fans-arrow" />
          </div>
          <div class="fans-main">
            <span class="fans-big">{{ followerCount }}</span>
            <span class="fans-unit">总粉丝</span>
          </div>
          <div class="fans-new" v-if="newFollowerCount > 0">
            <Icon icon="ph:user-plus" class="fans-new-icon" />
            <span>今日新增 {{ newFollowerCount }} 人</span>
          </div>
          <div class="fans-new fans-new--empty" v-else>
            <span>今日暂无新增粉丝</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import StatCard from '@/components/StatCard.vue'
import { usePostStats } from '@/composables/usePostStats'
import { useUserStore } from '@/stores/user'
import { getUserPosts, getTodayStats, type PostVO, type TodayStats } from '@/api/post'
import { getFollowStats } from '@/api/user'
import { getManagedComments } from '@/api/comment'
import { isDraft } from '@/utils/postStatus'

const emit = defineEmits<{
  goCreate: []
  goData: []
  goContent: [tab: 'draft' | 'all']
  goFans: []
}>()

const userStore = useUserStore()
const postStatsState = usePostStats()
const dataStats = postStatsState.stats

const followerCount = ref(0)
const newFollowerCount = ref(0)
const commentsTotal = ref(0)
const todayStats = ref<TodayStats>({ todayLikes: 0, todayCollections: 0, todayComments: 0 })
const posts = ref<PostVO[]>([])

const draftCount = computed(() => posts.value.filter(p => isDraft(p.status)).length)

const greetingText = computed(() => {
  const nickname = (userStore.userInfo as any)?.nickname || ''
  const h = new Date().getHours()
  const period = h < 12 ? '上午好' : h < 18 ? '下午好' : '晚上好'
  return nickname ? `${period}，${nickname}` : `${period}`
})

const iconWorks = 'ph:pencil-simple'
const iconEye = 'ph:eye'
const iconLike = 'ph:heart'
const iconFavorite = 'ph:star'
const iconFans = 'ph:users'
const iconComment = 'ph:chat-circle-text'

const goData = () => emit('goData')
const goDraft = () => emit('goContent', 'draft')
const goFans = () => emit('goFans')

const loadPosts = async () => {
  try {
    const data = await getUserPosts({ page: 1, size: 100 })
    posts.value = data.records || []
  } catch { /* ignore */ }
}

const loadFollowStats = async () => {
  try {
    const stats = await getFollowStats()
    followerCount.value = stats.followerCount || 0
    newFollowerCount.value = stats.newFollowerCount || 0
  } catch { /* ignore */ }
}

const loadCommentsTotal = async () => {
  try {
    const data = await getManagedComments({ page: 1, size: 1 })
    commentsTotal.value = data.total || 0
  } catch { /* ignore */ }
}

const loadTodayStats = async () => {
  try {
    todayStats.value = await getTodayStats()
  } catch { /* ignore */ }
}

onMounted(() => {
  postStatsState.loadMyStats()
  loadPosts()
  loadFollowStats()
  loadCommentsTotal()
  loadTodayStats()
})
</script>

<style scoped>
.creator-home {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.home-hero {
  background: linear-gradient(135deg, #fde4f0 0%, #f5e6ff 60%, #ece4ff 100%);
  border: 1px solid rgba(255, 107, 157, 0.15);
  border-radius: 18px;
}

html.dark .home-hero {
  background: linear-gradient(135deg, #2d1a2e 0%, #252040 60%, #1e1c38 100%);
  border-color: rgba(255, 107, 157, 0.1);
}

.hero-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
}

.hero-greeting {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.greeting-text {
  font-size: 14px;
  color: var(--text-dim);
  font-weight: 500;
}

.greeting-guide {
  font-size: 20px;
  font-weight: 800;
  color: var(--text);
  margin: 0;
}

.hero-publish-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: var(--white);
  font-size: 14px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  flex-shrink: 0;
}

.hero-publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(255, 107, 157, 0.35);
}

.btn-icon {
  width: 18px;
  height: 18px;
}

.card-panel,
.stats-panel {
  border: 1px solid var(--border-light);
  border-radius: 16px;
  background: var(--card);
  box-shadow: 0 2px 10px rgba(180, 132, 255, 0.04);
}

.stats-panel {
  padding: 20px 20px 18px;
}

.card-panel {
  padding: 20px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-header--compact {
  margin-bottom: 14px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 3px;
}

.section-desc {
  font-size: 12px;
  color: var(--text-dim);
  margin: 0;
}

.panel-link {
  font-size: 12px;
  color: var(--purple);
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.panel-link:hover {
  opacity: 0.7;
}

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 290px;
  grid-template-areas:
    'stats side'
    'pending side';
  gap: 20px;
  align-items: start;
}

.stats-panel {
  grid-area: stats;
}

.pending-panel {
  grid-area: pending;
}

.dashboard-side {
  grid-area: side;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stats-grid :deep(.stat-card) {
  border: 1px solid var(--border-light);
  box-shadow: none;
}

.stats-grid :deep(.stat-card:hover) {
  border-color: rgba(255, 107, 157, 0.15);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(180, 132, 255, 0.06);
}

.stats-grid :deep(.views-icon--primary .stat-icon-wrapper) {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.12), rgba(180, 132, 255, 0.1));
}

.pending-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.pending-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background: var(--bg-soft);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.pending-card.has-count {
  cursor: pointer;
  background: var(--card);
}

.pending-card.has-count:hover {
  border-color: rgba(255, 107, 157, 0.2);
  box-shadow: 0 4px 16px rgba(180, 132, 255, 0.08);
  transform: translateY(-2px);
}

.pending-card--disabled {
  opacity: 0.55;
  pointer-events: none;
}

.pending-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pending-icon {
  width: 20px;
  height: 20px;
}

.pending-draft .pending-icon-wrap {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.pending-content {
  min-width: 0;
  flex: 1;
}

.pending-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.pending-label {
  font-size: 13px;
  color: var(--text-dim);
}

.pending-num {
  font-size: 22px;
  font-weight: 800;
  color: var(--text);
  line-height: 1;
}

.pending-sub {
  font-size: 11px;
  color: var(--text-dim);
  line-height: 1.5;
}

.today-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.today-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  transition: background 0.18s ease;
}

.today-row:hover {
  background: var(--pink-bg);
}

.today-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.today-dot {
  width: 16px;
  height: 16px;
}

.likes-dot { color: #ec4899; }
.collects-dot { color: #a855f7; }
.comments-dot { color: #6366f1; }

.today-row-label {
  font-size: 13px;
  color: var(--text-dim);
}

.today-row-val {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
}

.fans-panel {
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.fans-panel:hover {
  border-color: rgba(255, 107, 157, 0.15);
  box-shadow: 0 4px 16px rgba(180, 132, 255, 0.06);
  transform: translateY(-2px);
}

.fans-arrow {
  width: 16px;
  height: 16px;
  color: var(--text-dim);
  margin-top: 4px;
}

.fans-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.fans-big {
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.fans-unit {
  font-size: 13px;
  color: var(--text-dim);
}

.fans-new {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 7px 10px;
  border-radius: 10px;
  font-size: 12px;
  color: var(--pink);
  background: var(--pink-bg);
}

.fans-new--empty {
  color: var(--text-dim);
  background: var(--bg-soft);
}

.fans-new-icon {
  width: 14px;
  height: 14px;
}

@media (max-width: 1100px) {
  .dashboard-layout {
    grid-template-columns: 1fr;
    grid-template-areas:
      'stats'
      'pending'
      'side';
  }

  .dashboard-side {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .hero-main {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding: 20px;
  }

  .hero-publish-btn {
    width: 100%;
    justify-content: center;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .pending-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-side {
    grid-template-columns: 1fr;
  }
}
</style>
