<template>
  <div class="creator-home">
    <div class="home-hero">
      <div class="hero-info">
        <h1>创作中心</h1>
        <p>记录你的灵感瞬间，发布图文作品，和同好一起交流</p>
      </div>
      <button class="hero-publish-btn" @click="$emit('goCreate')">
        <Icon icon="ph:pencil-simple" class="btn-icon pink-icon" />
        <span>发布新作品</span>
      </button>
    </div>

    <div class="stats-section">
      <h3 class="section-title">数据概览</h3>
      <div class="stats-grid">
        <StatCard icon-class="works-icon" :icon="iconEdit" :value="dataStats?.totalPosts ?? 0" label="总作品" />
        <StatCard icon-class="views-icon" :icon="iconEye" :value="dataStats?.totalViews ?? 0" label="总浏览" />
        <StatCard icon-class="likes-icon" :icon="iconLike" :value="dataStats?.totalLikes ?? 0" label="总点赞" />
        <StatCard icon-class="collections-icon" :icon="iconFavorite" :value="dataStats?.totalCollections ?? 0" label="总收藏" />
        <StatCard icon-class="fans-icon" :icon="iconFans" :value="followerCount" label="粉丝数" />
        <StatCard icon-class="comments-icon" :icon="iconComment" :value="commentsTotal" label="评论数" />
      </div>
    </div>

    <div class="recent-section" v-if="recentPosts.length > 0">
      <h3 class="section-title">最近作品</h3>
      <div class="recent-posts-list">
        <div class="recent-post-item" v-for="post in recentPosts" :key="post.id" @click="viewPost(post.id)">
          <div class="recent-post-cover">
            <img v-if="post.cover" :src="post.cover" alt="" />
            <div v-else class="cover-placeholder-small">📷</div>
          </div>
          <div class="recent-post-info">
            <h4 class="recent-post-title">{{ post.title }}</h4>
            <span class="recent-post-time">{{ formatTime(post.createTime) }}</span>
          </div>
          <div class="recent-post-stats">
            <span class="stat-item"><Icon icon="ph:eye" class="stat-mini-icon pink-icon" />{{ post.views }}</span>
            <span class="stat-item"><Icon icon="ph:heart" class="stat-mini-icon pink-icon" />{{ post.likes }}</span>
            <span class="stat-item"><Icon icon="ph:star" class="stat-mini-icon pink-icon" />{{ post.collections }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="recent-section">
      <h3 class="section-title">热门作品</h3>
      <div class="ranking-list">
        <div class="ranking-item" v-for="(item, index) in rankingList" :key="item.id" @click="viewPost(item.id)">
          <div class="rank-num" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
          <div class="rank-cover">
            <img v-if="item.cover" :src="item.cover" alt="" />
            <div v-else class="cover-placeholder-small">📷</div>
          </div>
          <div class="rank-info">
            <h4>{{ item.title }}</h4>
            <span class="rank-time">{{ formatDateTime(item.createTime) }}</span>
            <div class="rank-stats">
              <span class="stat-item"><Icon icon="ph:eye" class="stat-mini-icon pink-icon" />{{ item.views }}</span>
              <span class="stat-item"><Icon icon="ph:heart" class="stat-mini-icon pink-icon" />{{ item.likes }}</span>
              <span class="stat-item"><Icon icon="ph:star" class="stat-mini-icon pink-icon" />{{ item.collections }}</span>
            </div>
          </div>
        </div>
        <EmptyState v-if="rankingList.length === 0" title="还没有发布作品" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useNavigate } from '@/composables/useNavigate'
import StatCard from '@/components/StatCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { usePostStats } from '@/composables/usePostStats'
import { getUserPosts, getTopPosts } from '@/api/post'
import type { PostVO } from '@/api/post'
import { getFollowStats } from '@/api/user'
import { getManagedComments } from '@/api/comment'
import { formatTime, formatDateTime } from '@/utils/format'
import { isPublished } from '@/utils/postStatus'

defineEmits<{
  goCreate: []
}>()

const { open } = useNavigate()

const postStatsState = usePostStats()
const dataStats = postStatsState.stats

const followerCount = ref(0)
const commentsTotal = ref(0)

const posts = ref<PostVO[]>([])
const rankingList = ref<PostVO[]>([])

const iconEdit = 'ph:pencil-simple'
const iconEye = 'ph:eye'
const iconLike = 'ph:heart'
const iconFavorite = 'ph:star'
const iconFans = 'ph:users'
const iconComment = 'ph:chat-circle-text'

const recentPosts = computed(() => {
  return posts.value
    .filter(p => isPublished(p.status))
    .slice(0, 3)
})

const viewPost = (postId: string) => {
  open(`/post/${postId}`)
}

const loadPosts = async () => {
  try {
    const data = await getUserPosts({ page: 1, size: 100 })
    posts.value = data.records || []
  } catch (error) {
    console.error('加载帖子失败:', error)
  }
}

const loadRanking = async () => {
  try {
    rankingList.value = await getTopPosts(5) || []
  } catch (error) {
    console.error('加载排行榜失败:', error)
  }
}

const loadFollowStats = async () => {
  try {
    const stats = await getFollowStats()
    followerCount.value = stats.followerCount || 0
  } catch (error) {
    console.error('加载关注统计失败:', error)
  }
}

const loadCommentsTotal = async () => {
  try {
    const data = await getManagedComments({ page: 1, size: 1 })
    commentsTotal.value = data.total || 0
  } catch (error) {
    console.error('加载评论统计失败:', error)
  }
}

onMounted(() => {
  postStatsState.loadMyStats()
  loadPosts()
  loadRanking()
  loadFollowStats()
  loadCommentsTotal()
})
</script>

<style scoped>
.creator-home {
  /* 容器无需额外样式，父级 .main-content 已有 padding */
}

/* 创作首页 Hero 区域 */
.home-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 32px;
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
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 8px;
}

.hero-info p {
  font-size: 14px;
  opacity: 0.9;
}

.hero-publish-btn {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: 12px;
  background: var(--card);
  color: var(--pink);
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.hero-publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.hero-publish-btn .btn-icon {
  width: 20px;
  height: 20px;
}

.stats-section {
  margin-bottom: 32px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 220px));
  gap: 14px;
  justify-content: start;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 16px;
}

/* 首页：最近作品 */
.recent-section {
  margin-bottom: 28px;
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
  cursor: pointer;
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
}

.recent-post-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recent-post-info {
  flex: 1;
  min-width: 0;
}

.recent-post-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recent-post-time {
  font-size: 12px;
  color: var(--text-dim);
}

.recent-post-stats {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-dim);
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-dim);
  line-height: 1;
}

.stat-mini-icon {
  width: 14px;
  height: 14px;
  display: block;
  flex-shrink: 0;
  position: relative;
  top: 1px;
}

/* 排行榜 */
.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--card);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.2s;
}

.ranking-item:hover {
  border-color: var(--border);
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.1);
}

.rank-num {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  background: var(--border);
  color: var(--text-dim);
}

.rank-num.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffb700);
  color: white;
}

.rank-num.rank-2 {
  background: linear-gradient(135deg, #c0c0c0, #a0a0a0);
  color: white;
}

.rank-num.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #b87333);
  color: white;
}

.rank-cover {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.rank-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder-small {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f0e6ff, #ffe6f0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-time {
  display: block;
  font-size: 12px;
  color: var(--text-dim);
  margin-bottom: 4px;
}

.rank-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-dim);
}

.rank-stats .stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 1;
}
</style>
