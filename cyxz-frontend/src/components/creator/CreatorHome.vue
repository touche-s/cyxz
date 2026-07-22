<template>
  <div class="creator-home">
    <div class="home-hero">
      <div class="hero-info">
        <h1>创作中心</h1>
        <p>记录你的灵感瞬间，发布图文作品，和同好一起交流</p>
      </div>
      <button class="hero-publish-btn" @click="$emit('goCreate')">
        <Icon icon="ph:pencil-simple" class="btn-icon" />
        <span>发布新作品</span>
      </button>
    </div>

    <!-- 数据概览：4 张核心卡片 -->
    <div class="stats-section">
      <div class="stats-header">
        <h3 class="section-title">数据概览</h3>
        <span class="stats-hint" @click="goData">查看完整趋势 →</span>
      </div>
      <div class="stats-grid">
        <StatCard icon-class="views-icon" :icon="iconEye" :value="dataStats?.totalViews ?? 0" label="总浏览" />
        <StatCard icon-class="likes-icon" :icon="iconLike" :value="dataStats?.totalLikes ?? 0" label="总点赞" />
        <StatCard icon-class="collections-icon" :icon="iconFavorite" :value="dataStats?.totalCollections ?? 0" label="总收藏" />
        <StatCard icon-class="fans-icon" :icon="iconFans" :value="followerCount" label="粉丝数" />
      </div>
    </div>

    <!-- 待处理事项 -->
    <div class="pending-section">
      <h3 class="section-title">待处理</h3>
      <div class="pending-grid">
        <div class="pending-card pending-audit" @click="goAudit">
          <Icon icon="ph:clipboard-text" class="pending-icon" />
          <div class="pending-info">
            <span class="pending-label">待审核作品</span>
            <span class="pending-num">{{ auditCount }}</span>
          </div>
        </div>
        <div class="pending-card pending-comments" @click="goInteraction">
          <Icon icon="ph:chat-circle-dots" class="pending-icon" />
          <div class="pending-info">
            <span class="pending-label">新评论</span>
            <span class="pending-num">{{ todayStats.todayComments }}</span>
          </div>
          <span class="pending-badge" v-if="todayStats.todayComments > 0">{{ todayStats.todayComments }}条待查看</span>
        </div>
        <div class="pending-card pending-fans" @click="goFans">
          <Icon icon="ph:user-plus" class="pending-icon" />
          <div class="pending-info">
            <span class="pending-label">新粉丝</span>
            <span class="pending-num">{{ newFollowerCount }}</span>
          </div>
        </div>
        <div class="pending-card pending-draft" @click="goDraft" v-if="draftCount > 0">
          <Icon icon="ph:note-pencil" class="pending-icon" />
          <div class="pending-info">
            <span class="pending-label">草稿箱</span>
            <span class="pending-num">{{ draftCount }}</span>
          </div>
          <span class="pending-draft-hint">完善后即可发布</span>
        </div>
      </div>
    </div>

    <!-- 今日新增 -->
    <div class="today-section">
      <h3 class="section-title">今日新增</h3>
      <div class="today-cards">
        <div class="today-card">
          <Icon icon="ph:heart" class="today-icon likes-icon" />
          <span class="today-value">{{ todayStats.todayLikes }}</span>
          <span class="today-label">点赞</span>
        </div>
        <div class="today-card">
          <Icon icon="ph:star" class="today-icon collects-icon" />
          <span class="today-value">{{ todayStats.todayCollections }}</span>
          <span class="today-label">收藏</span>
        </div>
      </div>
    </div>

    <!-- 最近作品 -->
    <div class="recent-section" v-if="recentPosts.length > 0">
      <div class="recent-header">
        <h3 class="section-title">最近作品</h3>
        <span class="recent-all" @click="goAllContent">全部作品 →</span>
      </div>
      <div class="recent-posts-list">
        <div class="recent-post-item" v-for="post in recentPosts" :key="post.id">
          <div class="recent-post-cover" @click="viewPost(post.id)">
            <img v-if="post.cover" :src="post.cover" alt="" />
            <div v-else class="cover-placeholder-small">📷</div>
          </div>
          <div class="recent-post-info" @click="viewPost(post.id)">
            <h4 class="recent-post-title">{{ post.title }}</h4>
            <div class="recent-post-meta">
              <span class="post-status-tag" :class="'tag-' + post.status">{{ statusText(post.status) }}</span>
              <span>{{ post.views }}浏览 · {{ post.likes }}赞 · {{ formatTime(post.createTime) }}</span>
            </div>
          </div>
          <div class="recent-post-actions">
            <button class="post-action-btn" @click.stop="editPost(post.id)" title="编辑">
              <Icon icon="ph:pencil-simple" class="action-iconify" />
              <span>编辑</span>
            </button>
            <button class="post-action-btn" @click.stop="viewPostData(post.id)" title="数据" v-if="isPublished(post.status)">
              <Icon icon="ph:chart-line" class="action-iconify" />
              <span>数据</span>
            </button>
            <button class="post-action-btn post-action-more" @click.stop="showMoreOptions(post)" title="更多">
              <Icon icon="ph:dots-three" class="action-iconify" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useNavigate } from '@/composables/useNavigate'
import StatCard from '@/components/StatCard.vue'
import { usePostStats } from '@/composables/usePostStats'
import { getUserPosts, getTodayStats, type PostVO, type TodayStats } from '@/api/post'
import { getFollowStats } from '@/api/user'
import { formatTime } from '@/utils/format'
import { isPublished, isDraft, statusText } from '@/utils/postStatus'

const emit = defineEmits<{
  goCreate: []
  goData: []
  goContent: [tab: 'draft' | 'all']
  goInteraction: []
  goFans: []
  editPost: [postId: string]
}>()

const { open } = useNavigate()

const postStatsState = usePostStats()
const dataStats = postStatsState.stats

const followerCount = ref(0)
const newFollowerCount = ref(0)
const auditCount = ref(0)
const todayStats = ref<TodayStats>({ todayLikes: 0, todayCollections: 0, todayComments: 0 })

const posts = ref<PostVO[]>([])

const draftCount = computed(() => posts.value.filter(p => isDraft(p.status)).length)

const iconEye = 'ph:eye'
const iconLike = 'ph:heart'
const iconFavorite = 'ph:star'
const iconFans = 'ph:users'

const recentPosts = computed(() => posts.value.slice(0, 5))

const viewPost = (postId: string) => open(`/post/${postId}`)
const editPost = (postId: string) => emit('editPost', postId)
const viewPostData = (postId: string) => open(`/post/${postId}`)
const showMoreOptions = (post: PostVO) => {
  // TODO: dropdown menu with delete/archive/link copy
}

const goData = () => emit('goData')
const goAllContent = () => emit('goContent', 'all')
const goDraft = () => emit('goContent', 'draft')
const goInteraction = () => emit('goInteraction')
const goFans = () => emit('goFans')
const goAudit = () => {} // TODO: 审核功能上线后对接

const loadPosts = async () => {
  try {
    const data = await getUserPosts({ page: 1, size: 100 })
    posts.value = data.records || []
  } catch (error) {
    console.error('加载帖子失败:', error)
  }
}

const loadFollowStats = async () => {
  try {
    const stats = await getFollowStats()
    followerCount.value = stats.followerCount || 0
    newFollowerCount.value = stats.newFollowerCount || 0
  } catch (error) {
    console.error('加载关注统计失败:', error)
  }
}

const loadTodayStats = async () => {
  try {
    todayStats.value = await getTodayStats()
  } catch (error) {
    console.error('加载今日统计失败:', error)
  }
}

onMounted(() => {
  postStatsState.loadMyStats()
  loadPosts()
  loadFollowStats()
  loadTodayStats()
})
</script>

<style scoped>
.home-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding: 28px 32px;
  background: linear-gradient(135deg, var(--pink) 0%, var(--purple) 100%);
  border-radius: 16px;
  color: white;
  position: relative;
  overflow: hidden;
}

.home-hero::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.home-hero::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.hero-info {
  position: relative;
  z-index: 1;
}

.hero-info h1 {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 6px;
}

.hero-info p {
  font-size: 13px;
  opacity: 0.85;
}

.hero-publish-btn {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  border-radius: 14px;
  background: #fff;
  color: var(--pink);
  font-size: 15px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.hero-publish-btn:hover {
  transform: translateY(-3px) scale(1.03);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.15);
}

.hero-publish-btn .btn-icon {
  width: 20px;
  height: 20px;
}

/* 数据概览 */
.stats-section {
  margin-bottom: 24px;
}

.stats-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
}

.stats-hint {
  font-size: 12px;
  color: var(--purple);
  cursor: pointer;
  transition: opacity 0.15s;
}

.stats-hint:hover {
  opacity: 0.7;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

/* 待处理 */
.pending-section {
  margin-bottom: 24px;
}

.pending-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.pending-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--card);
  cursor: pointer;
  transition: all 0.18s;
  position: relative;
  overflow: hidden;
}

.pending-card:hover {
  border-color: var(--purple-light, #d8b4fe);
  box-shadow: 0 2px 10px rgba(192, 132, 252, 0.08);
}

.pending-icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.pending-audit .pending-icon { color: #f59e0b; }
.pending-comments .pending-icon { color: #6366f1; }
.pending-fans .pending-icon { color: #ec4899; }
.pending-draft .pending-icon { color: #8b5cf6; }

.pending-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pending-label {
  font-size: 12px;
  color: var(--text-dim);
}

.pending-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  line-height: 1;
}

.pending-badge {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 10px;
  color: #fff;
  background: #ef4444;
  padding: 2px 7px;
  border-radius: 8px;
  font-weight: 600;
}

.pending-draft-hint {
  font-size: 10px;
  color: var(--text-dim);
  white-space: nowrap;
}

/* 今日新增 */
.today-section {
  margin-bottom: 24px;
}

.today-cards {
  display: flex;
  gap: 12px;
}

.today-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--card);
  transition: all 0.15s;
}

.today-card:hover {
  border-color: var(--pink);
  box-shadow: 0 2px 8px rgba(192, 132, 252, 0.06);
}

.today-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.today-icon.likes-icon { color: #ec4899; }
.today-icon.collects-icon { color: #a855f7; }

.today-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  line-height: 1;
}

.today-label {
  font-size: 12px;
  color: var(--text-dim);
  margin-left: auto;
}

/* 板块标题 */
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 12px;
}

/* 最近作品 */
.recent-section {
  margin-bottom: 24px;
}

.recent-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.recent-all {
  font-size: 12px;
  color: var(--purple);
  cursor: pointer;
}

.recent-all:hover {
  opacity: 0.7;
}

.recent-posts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recent-post-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  transition: all 0.2s;
}

.recent-post-item:hover {
  border-color: var(--border);
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.1);
}

.recent-post-cover {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-secondary);
  cursor: pointer;
}

.recent-post-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recent-post-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.recent-post-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.recent-post-meta {
  font-size: 12px;
  color: var(--text-dim);
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-status-tag {
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 5px;
  font-weight: 600;
}

.tag-0 { background: rgba(245, 158, 11, 0.12); color: #d97706; }
.tag-1 { background: rgba(16, 185, 129, 0.12); color: #059669; }

.recent-post-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.post-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--text-dim);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.post-action-btn:hover {
  border-color: var(--purple-light, #d8b4fe);
  color: var(--purple);
  background: rgba(192, 132, 252, 0.06);
}

.post-action-btn .action-iconify {
  width: 14px;
  height: 14px;
}

.post-action-more {
  padding: 6px 8px;
}

.cover-placeholder-small {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
</style>
