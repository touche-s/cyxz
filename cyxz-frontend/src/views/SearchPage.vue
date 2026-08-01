<template>
  <div class="search-page">
    <div class="search-header">
      <div class="search-header-inner">
        <SearchInput
          v-model="keyword"
          variant="page"
          placeholder="搜索感兴趣的内容..."
          @search="doSearch"
        />
        <button class="search-btn" @click="doSearch">搜索</button>
      </div>
    </div>

    <div class="search-main">
      <div class="search-result-info" v-if="searched">
        <template v-if="total > 0">
          找到 <strong>{{ total }}</strong> 条关于「<em>{{ searchedKeyword }}</em>」的结果
        </template>
        <template v-else>
          未找到关于「<em>{{ searchedKeyword }}</em>」的结果
        </template>
      </div>

      <div class="content-grid" v-if="results.length">
        <PostCard
          v-for="item in results"
          :key="item.id"
          :post="item"
          :show-like="true"
          @click="goToPost"
        />
      </div>

      <EmptyState v-else-if="!loading && searched" title="没有找到相关内容" hint="试试换一个关键词吧~" />

      <LoadingSpinner v-if="loading" text="搜索中..." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useNavigate } from '@/composables/useNavigate'
import { searchPostsEs } from '@/api/search'
import type { PostSearchVO } from '@/api/search'
import type { PostVO } from '@/api/post'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import SearchInput from '@/components/SearchInput.vue'

const route = useRoute()
const { open, router } = useNavigate()

const keyword = ref('')
const searchedKeyword = ref('')
const searched = ref(false)
const loading = ref(false)
const results = ref<PostVO[]>([])
const total = ref(0)

/** ES 搜索结果 → PostCard 兼容格式 */
function mapToPostVO(r: PostSearchVO): PostVO {
  return {
    id: String(r.id),
    userId: String(r.userId || ''),
    authorName: '',
    authorAvatar: '',
    postType: r.postType || 'NORMAL',
    sectionId: r.sectionId || 0,
    sectionName: '',
    circleId: r.circleId || 0,
    circleName: '',
    title: r.title || '',
    content: r.content || '',
    cover: r.cover || '',
    images: [],
    tags: r.tags || [],
    status: r.status || 0,
    likes: r.likes || 0,
    comments: r.comments || 0,
    views: r.views || 0,
    collections: r.collections || 0,
    liked: false,
    collected: false,
    pinned: false,
    pinnedTime: '',
    createTime: r.createTime ? String(r.createTime) : '',
    updateTime: '',
  }
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  searchedKeyword.value = kw
  router.replace({ query: { q: kw } })
  loading.value = true
  searched.value = true
  try {
    const data = await searchPostsEs({ keyword: kw, page: 1, size: 30 })
    results.value = (data.records || []).map(mapToPostVO)
    total.value = data.total || 0
  } catch {
    results.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function goToPost(post: PostVO) {
  open(`/post/${post.id}`)
}

onMounted(async () => {
  const q = route.query.q as string
  if (q) {
    keyword.value = q
    doSearch()
  }
})
</script>

<style scoped>
.search-page {
  min-height: calc(100vh - 60px);
  background: var(--bg-alt);
  padding-bottom: 60px;
}

.search-header {
  background: transparent;
  border-bottom: none;
  padding: 22px 0 0;
  position: sticky;
  top: 60px;
  z-index: 10;
}

.search-header-inner {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-btn {
  padding: 10px 28px;
  background: var(--pink);
  color: var(--white);
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
  white-space: nowrap;
}

.search-btn:hover {
  opacity: 0.85;
}

.search-main {
  width: min(1368px, calc(100vw - 48px));
  margin: 0 auto;
  padding: 32px 0 24px;
}

.search-result-info {
  font-size: 14px;
  color: var(--text-dim);
  margin-top: 36px;
  margin-bottom: 24px;
}

.search-result-info strong {
  color: var(--pink);
  font-weight: 600;
}

.search-result-info em {
  font-style: normal;
  color: var(--pink);
  font-weight: 500;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

@media (max-width: 1000px) {
  .content-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 700px) {
  .content-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
