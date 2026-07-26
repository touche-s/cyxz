<template>
  <main class="main-content">
    <div class="page-inner">
      <div class="discover-header">
        <h1 class="discover-title">发现</h1>
        <p class="discover-sub">{{ currentSubtitle }}</p>
      </div>

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

      <EmptyState v-if="!loading && posts.length === 0" title="新的灵感正在路上" description="换个时间再来看看吧~" />

    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { getPostList, likePost, unlikePost, collectPost, uncollectPost } from '@/api/post'
import type { PostVO } from '@/api/post'
import { useNavigate } from '@/composables/useNavigate'
import { useAuth } from '@/composables/useAuth'
import UnderlineTabs from '@/components/UnderlineTabs.vue'
import PostCard from '@/components/PostCard.vue'
import MasonryGrid from '@/components/MasonryGrid.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import { estimatePostHeight } from '@/utils/format'

const { open } = useNavigate()
const { requireLogin } = useAuth()

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

const toggleSave = async (post: PostVO) => {
  if (!requireLogin()) return

  const oldCollected = post.collected
  const oldCollections = post.collections

  post.collected = !oldCollected
  post.collections = oldCollected ? Math.max(oldCollections - 1, 0) : oldCollections + 1

  try {
    if (oldCollected) {
      await uncollectPost(post.id)
    } else {
      await collectPost(post.id)
    }
  } catch {
    post.collected = oldCollected
    post.collections = oldCollections
  }
}

const handlePostLike = async (post: PostVO) => {
  if (!requireLogin()) return

  const oldLiked = post.liked
  const oldLikes = post.likes

  post.liked = !oldLiked
  post.likes = oldLiked ? Math.max(oldLikes - 1, 0) : oldLikes + 1

  try {
    if (oldLiked) {
      await unlikePost(post.id)
    } else {
      await likePost(post.id)
    }
  } catch {
    post.liked = oldLiked
    post.likes = oldLikes
  }
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

@media (max-width: 768px) {
  .main-content { padding: 86px 0 60px; }
}
</style>
