<template>
  <main class="main-content">
    <div class="page-inner">
      <div class="discover-header">
        <h1 class="discover-title">次元街</h1>
        <p class="discover-sub">{{ currentSubtitle }}</p>
      </div>

      <!-- 本周话题 -->
      <section class="topics-section">
        <div class="topics-head">
          <div class="topics-head-left">
            <Icon icon="ph:lightning" class="topics-head-icon" />
            <span class="topics-head-title">本周活动话题</span>
          </div>
          <span class="topics-head-hint">参与话题讨论，找到同好</span>
        </div>
        <div class="topics-grid">
          <div
            v-for="topic in weeklyTopics"
            :key="topic.id"
            class="topic-card"
            @click="goToTopic(topic)"
          >
            <div class="topic-cover">
              <Icon :icon="topic.icon" class="topic-cover-icon" />
            </div>
            <div class="topic-info">
              <span class="topic-label">{{ topic.label }}</span>
              <h4 class="topic-title">{{ topic.title }}</h4>
              <p class="topic-desc">{{ topic.desc }}</p>
              <div class="topic-meta">
                <span class="topic-circle-tag">{{ topic.circleName }}</span>
                <span class="topic-count">{{ topic.postCount }} 篇参与</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <UnderlineTabs v-model="activeSort" :tabs="sortTabs" />

      <div class="content-grid">
        <MasonryGrid
          :items="posts"
          :column-count="4"
          :gap="18"
          :estimate-height="estimatePostHeight"
        >
          <template #item="{ item }">
            <PostCard
              :post="item"
              :show-collect="true"
              :show-like="true"
              @click="viewPost"
              @like="handlePostLike"
              @collect="toggleSave"
            />
          </template>
        </MasonryGrid>
      </div>

      <LoadingSpinner v-if="loading" text="加载中..." />

      <EmptyState v-if="!loading && posts.length === 0" icon="ph:sparkle" title="新的灵感正在路上" hint="换个时间再来看看吧~" />

    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { getPostList, likePost, unlikePost, collectPost, uncollectPost } from '@/api/post'
import type { PostVO } from '@/api/post'
import { useNavigate } from '@/composables/useNavigate'
import { useAuth } from '@/composables/useAuth'
import { createToggleAction } from '@/composables/useToggleInteraction'
import UnderlineTabs from '@/components/UnderlineTabs.vue'
import PostCard from '@/components/PostCard.vue'
import MasonryGrid from '@/components/MasonryGrid.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import { estimatePostHeight } from '@/utils/format'

const { open } = useNavigate()
const router = useRouter()
const { requireLogin } = useAuth()

interface WeeklyTopic {
  id: number
  icon: string
  label: string
  title: string
  desc: string
  circleName: string
  circleId: number
  postCount: number
}

const weeklyTopics: WeeklyTopic[] = [
  { id: 1, icon: 'ph:sword', label: '攻略挑战', title: '深渊12层低配通关挑战', desc: '用四星角色/低命阵容通关深渊，分享配队思路', circleName: '原神', circleId: 2, postCount: 23 },
  { id: 2, icon: 'ph:rocket-launch', label: '剧情讨论', title: '翁法罗斯篇章结局预测', desc: '星穹列车下一站会揭示什么？写下你的剧情猜想', circleName: '崩坏：星穹铁道', circleId: 3, postCount: 41 },
  { id: 3, icon: 'ph:paint-brush', label: '同人创作', title: '夏日祭同人创作季', desc: '用画笔和文字描绘你最喜欢的角色夏日日常', circleName: '同人创作圈', circleId: 20, postCount: 35 },
  { id: 4, icon: 'ph:chat-circle-dots', label: '作品安利', title: '一句台词安利你的本命番', desc: '发帖安利你最喜欢的动漫/游戏，用图说话', circleName: '日常交流圈', circleId: 21, postCount: 17 },
]

function goToTopic(topic: WeeklyTopic) {
  router.push(`/circle/${topic.circleId}`)
}

const sortTabs = [
  { key: 'recommend', label: '推荐', icon: 'ph:sparkle' },
  { key: 'hot', label: '热门', icon: 'ph:fire' },
  { key: 'latest', label: '最新', icon: 'ph:clock' },
]

const subtitles: Record<string, string> = {
  recommend: '看看大家最近都在讨论什么',
  hot: '近期最受欢迎的帖子合集',
  latest: '一起捕捉新鲜出炉的创意',
}

const posts = ref<PostVO[]>([])
const activeSort = ref('recommend')
const loading = ref(false)

const currentSubtitle = computed(() => subtitles[activeSort.value] || subtitles.recommend)

function sortByParam(): string {
  if (activeSort.value === 'recommend') return 'hot'
  return activeSort.value
}

const loadPosts = async () => {
  loading.value = true
  try {
    const params: any = { page: 1, size: 12, sortBy: sortByParam() }
    const data = await getPostList(params)
    posts.value = data.records || []
  } catch (error) {
    console.error('加载帖子失败:', error)
  } finally {
    loading.value = false
  }
}

watch(activeSort, () => loadPosts())

const viewPost = (post: PostVO) => {
  if (!requireLogin()) return
  open(`/post/${post.id}`)
}

const toggleLike = createToggleAction<PostVO>({
  likedField: 'liked',
  countField: 'likes',
  likeApi: (id) => likePost(id),
  unlikeApi: (id) => unlikePost(id),
  idGetter: (p) => p.id,
})

const toggleCollect = createToggleAction<PostVO>({
  likedField: 'collected',
  countField: 'collections',
  likeApi: (id) => collectPost(id),
  unlikeApi: (id) => uncollectPost(id),
  idGetter: (p) => p.id,
})

const handlePostLike = (post: PostVO) => {
  if (!requireLogin()) return
  toggleLike(post)
}

const toggleSave = (post: PostVO) => {
  if (!requireLogin()) return
  toggleCollect(post)
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.main-content {
  padding: 90px 0 60px;
}

.page-inner {
  width: min(1368px, calc(100vw - 48px));
  margin: 0 auto;
  padding: 0;
}

.discover-header {
  margin-bottom: 8px;
  padding: 0 0 0 4px;
}

.discover-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--text);
  margin: 0 0 4px;
}

.discover-sub {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
}

.content-grid {
  margin-bottom: 32px;
}

/* ===== Weekly Topics ===== */
.topics-section {
  margin-bottom: 24px;
}

.topics-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.topics-head-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.topics-head-icon {
  width: 18px;
  height: 18px;
  color: #f59e0b;
}

.topics-head-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
}

.topics-head-hint {
  font-size: 12px;
  color: var(--text-dim);
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.topic-card {
  background: var(--card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.22s ease;
  display: flex;
  gap: 14px;
}

.topic-card:hover {
  border-color: var(--pink);
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.08);
  transform: translateY(-2px);
}

.topic-cover {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--pink-bg), var(--purple-bg));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.topic-cover-icon {
  width: 22px;
  height: 22px;
  color: var(--pink);
}

.topic-info {
  flex: 1;
  min-width: 0;
}

.topic-label {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
  padding: 1px 8px;
  border-radius: 8px;
  margin-bottom: 6px;
}

.topic-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 4px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topic-desc {
  font-size: 12px;
  color: var(--text-dim);
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.topic-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  color: var(--text-dim);
}

.topic-circle-tag {
  padding: 2px 8px;
  background: var(--pink-bg);
  color: var(--pink);
  border-radius: 6px;
  font-weight: 500;
}

.topic-count {
  color: var(--text-dim);
}

@media (max-width: 1200px) {
  .topics-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 900px) {
  .topics-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .topics-grid { grid-template-columns: 1fr; }
  .topics-head { flex-direction: column; align-items: flex-start; gap: 4px; }
}

@media (max-width: 768px) {
  .main-content { padding: 86px 0 60px; }
}
</style>
