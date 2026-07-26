<template>
  <main class="main-content">
    <div class="page-inner" v-if="circle">

    <!-- 圈子头部 -->
    <div class="circle-header">
      <div class="circle-cover">
        <img v-if="circle.cover" :src="circle.cover" :alt="circle.name" />
        <div v-else class="cover-fallback"></div>
        <div class="cover-overlay"></div>
      </div>
      <div class="circle-header-body">
        <div class="circle-header-left">
          <div class="circle-avatar">
            <img v-if="circle.avatar" :src="circle.avatar" :alt="circle.name" />
            <span v-else class="avatar-text">{{ circle.name.charAt(0) }}</span>
          </div>
          <div class="circle-meta">
            <h1 class="circle-name">{{ circle.name }}</h1>
            <p class="circle-intro">{{ circle.intro }}</p>
            <div class="circle-stats">
              <span>{{ circle.postCount }} 帖子</span>
              <span class="stat-sep">·</span>
              <span>{{ circle.memberCount }} 成员</span>
            </div>
          </div>
        </div>
        <div class="circle-actions">
          <button class="btn-publish" @click="goPublish">
            <Icon icon="ph:pencil-simple" />
            圈内发布
          </button>
          <button class="btn-join" :class="{ joined: circle.joined }" :disabled="joinLoading" @click="toggleJoin">
            {{ circle.joined ? '已加入' : '加入' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 板块筛选 -->
    <PillTabs v-model="categoryTab" :tabs="categoryTabs" />

    <!-- 排序切换 -->
    <UnderlineTabs v-model="sortBy" :tabs="sortTabs" />

    <!-- 帖子列表 -->
    <div class="content-grid" v-if="!loading">
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

    <LoadingSpinner v-else text="加载中..." />

    <EmptyState v-if="!loading && posts.length === 0" title="这个圈子还没有内容" description="来做第一个发帖的人吧" />

    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import { getCircleDetail, joinCircle, leaveCircle } from '@/api/circle'
import type { CircleVO } from '@/api/circle'
import { getPostList, getCategoryList, likePost, unlikePost, collectPost, uncollectPost } from '@/api/post'
import type { PostVO, CategoryVO } from '@/api/post'
import { useNavigate } from '@/composables/useNavigate'
import { useAuth } from '@/composables/useAuth'
import { useUserStore } from '@/stores/user'
import PostCard from '@/components/PostCard.vue'
import UnderlineTabs from '@/components/UnderlineTabs.vue'
import PillTabs from '@/components/PillTabs.vue'
import MasonryGrid from '@/components/MasonryGrid.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import { estimatePostHeight } from '@/utils/format'

const route = useRoute()
const { open, to } = useNavigate()
const { requireLogin } = useAuth()
const userStore = useUserStore()

const circleId = Number(route.params.id)
const circle = ref<CircleVO | null>(null)
const posts = ref<PostVO[]>([])
const loading = ref(false)
const joinLoading = ref(false)
const sortBy = ref('latest')
const sortTabs = [
  { key: 'hot', label: '热门', icon: 'ph:fire' },
  { key: 'latest', label: '最新', icon: 'ph:clock' },
]
const categories = ref<CategoryVO[]>([])
const selectedCategoryId = ref<number | null>(null)

const categoryTab = ref('all')
const categoryTabs = computed(() => [
  { key: 'all', label: '全部' },
  ...categories.value.map(c => ({ key: String(c.id), label: c.name })),
])

watch(categoryTab, (val) => {
  selectedCategoryId.value = val === 'all' ? null : Number(val)
  loadPosts()
})

async function loadCircle() {
  try {
    circle.value = await getCircleDetail(circleId)
  } catch (e) {
    console.error('加载圈子失败:', e)
  }
}

async function loadCategories() {
  try {
    categories.value = await getCategoryList()
  } catch (e) {
    console.error('加载分类失败:', e)
  }
}

async function loadPosts() {
  loading.value = true
  try {
    const params: any = { circleId, sortBy: sortBy.value, page: 1, size: 12 }
    if (selectedCategoryId.value !== null) {
      params.categoryId = selectedCategoryId.value
    }
    const data = await getPostList(params)
    posts.value = data.records || []
  } catch (e) {
    console.error('加载帖子失败:', e)
  } finally {
    loading.value = false
  }
}

async function toggleJoin() {
  if (!requireLogin()) return
  if (!circle.value) return
  if (joinLoading.value) return
  const prevJoined = circle.value.joined
  const prevMemberCount = circle.value.memberCount
  joinLoading.value = true
  try {
    if (circle.value.joined) {
      await leaveCircle(circle.value.id)
      circle.value.joined = false
      circle.value.memberCount = Math.max(circle.value.memberCount - 1, 0)
    } else {
      await joinCircle(circle.value.id)
      circle.value.joined = true
      circle.value.memberCount = circle.value.memberCount + 1
    }
  } catch (e) {
    console.error('操作失败:', e)
    circle.value.joined = prevJoined
    circle.value.memberCount = prevMemberCount
  } finally {
    joinLoading.value = false
  }
}

function goPublish() {
  if (!requireLogin()) return
  userStore.creatorActiveNav = 'publish'
  userStore.pendingCircleId = circleId
  to('/creator')
}

function viewPost(post: PostVO) {
  if (!requireLogin()) return
  open(`/post/${post.id}`)
}

const toggleSave = async (post: PostVO) => {
  if (!requireLogin()) return
  const old = post.collected
  post.collected = !old
  post.collections = old ? Math.max(post.collections - 1, 0) : post.collections + 1
  try {
    old ? await uncollectPost(post.id) : await collectPost(post.id)
  } catch {
    post.collected = old
    post.collections = old ? post.collections + 1 : Math.max(post.collections - 1, 0)
  }
}

const handlePostLike = async (post: PostVO) => {
  if (!requireLogin()) return
  const old = post.liked
  post.liked = !old
  post.likes = old ? Math.max(post.likes - 1, 0) : post.likes + 1
  try {
    old ? await unlikePost(post.id) : await likePost(post.id)
  } catch {
    post.liked = old
    post.likes = old ? post.likes + 1 : Math.max(post.likes - 1, 0)
  }
}

watch(sortBy, () => loadPosts())

onMounted(() => {
  loadCircle()
  loadCategories()
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
}

/* ===== Circle Header ===== */
.circle-header {
  background: var(--card);
  border: 1.5px solid var(--border-light);
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(255, 107, 157, 0.04);
}

.circle-cover {
  position: relative;
  height: 170px;
  overflow: hidden;
  background: var(--gradient-card);
}

.circle-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-fallback {
  width: 100%;
  height: 100%;
  background: var(--gradient-card-hover);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    transparent 30%,
    rgba(255, 255, 255, 0.08) 60%,
    rgba(255, 255, 255, 0.35) 100%
  );
  pointer-events: none;
}

.circle-header-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px 24px;
  margin-top: -36px;
  position: relative;
  z-index: 1;
}

.circle-header-left {
  display: flex;
  align-items: flex-end;
  gap: 18px;
}

.circle-avatar {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.25);
  flex-shrink: 0;
  border: 3px solid var(--card);
}

.circle-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  color: white;
  font-size: 28px;
  font-weight: 800;
}

.circle-name {
  font-size: 22px;
  font-weight: 800;
  color: var(--text);
  margin: 0 0 6px 0;
}

.circle-intro {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0 0 10px 0;
}

.circle-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-dim);
}

.stat-sep {
  color: var(--border);
}

.circle-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.btn-publish {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 22px;
  border-radius: 14px;
  border: none;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s ease-out;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.25);
}

.btn-publish:hover {
  transform: scale(1.04);
  box-shadow: 0 6px 24px rgba(180, 132, 255, 0.35);
}

.btn-publish:active { transform: scale(0.97); }

.btn-join {
  padding: 8px 16px;
  border-radius: 12px;
  border: 1.5px solid var(--pink);
  background: transparent;
  color: var(--pink);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  align-self: center;
  transition: all 0.22s ease;
  white-space: nowrap;
}

.btn-join:hover {
  background: var(--pink);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.18);
}

.btn-join.joined {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.08), rgba(180, 132, 255, 0.06));
  border-color: var(--pink);
  color: var(--pink);
  box-shadow: 0 0 0 1px rgba(255, 107, 157, 0.08);
}

.btn-join.joined:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
  box-shadow: none;
}

.filter-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-toolbar :deep(.pill-tabs) {
  margin-bottom: 0;
}

.filter-toolbar :deep(.underline-tabs) {
  margin-bottom: 0;
  padding-bottom: 4px;
}

/* ===== Content Grid ===== */
.content-grid {
  margin-bottom: 32px;
}

@media (max-width: 768px) {
  .circle-cover { height: 120px; }
  .circle-header-body { flex-direction: column; gap: 16px; align-items: flex-start; padding: 0 16px 18px; margin-top: -28px; }
  .circle-avatar { width: 64px; height: 64px; }
}

/* ===== Dark mode overrides ===== */
html.dark .cover-overlay {
  background: linear-gradient(
    to bottom,
    transparent 30%,
    rgba(40, 35, 60, 0.08) 60%,
    rgba(40, 35, 60, 0.35) 100%
  );
}

html.dark .btn-join.joined:hover {
  background: rgba(239, 68, 68, 0.1);
}
</style>
