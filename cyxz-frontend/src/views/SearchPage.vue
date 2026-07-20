<template>
  <div class="search-page">
    <div class="search-header">
      <div class="search-header-inner">
        <div class="search-input-wrap">
          <img src="@/assets/icons/search.svg" alt="search" class="search-icon" />
          <input
            ref="searchInputRef"
            v-model="keyword"
            type="text"
            placeholder="搜索感兴趣的内容..."
            @keyup.enter="doSearch"
          />
        </div>
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
          @click="goToPost"
        />
      </div>

      <EmptyState v-else-if="!loading && searched" title="没有找到相关内容" hint="试试换一个关键词吧~" />

      <LoadingSpinner v-if="loading" text="搜索中..." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchPosts } from '@/api/post'
import type { PostVO } from '@/api/post'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const route = useRoute()
const router = useRouter()

const searchInputRef = ref<HTMLInputElement>()
const keyword = ref('')
const searchedKeyword = ref('')
const searched = ref(false)
const loading = ref(false)
const results = ref<PostVO[]>([])
const total = ref(0)

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  searchedKeyword.value = kw
  router.replace({ query: { q: kw } })
  loading.value = true
  searched.value = true
  try {
    const res = await searchPosts({ keyword: kw, page: 1, size: 30 })
    if (res.data.code === 200) {
      const data = res.data.data
      results.value = data.records || []
      total.value = data.total || 0
    }
  } catch {
    results.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function goToPost(post: PostVO) {
  const url = router.resolve(`/post/${post.id}`).href
  window.open(url, '_blank')
}

onMounted(async () => {
  const q = route.query.q as string
  if (q) {
    keyword.value = q
    await nextTick()
    doSearch()
  }
})
</script>

<style scoped>
.search-page {
  min-height: calc(100vh - 60px);
  background: #f5f0f8;
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

.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  transition: border-color 0.2s;
}

.search-input-wrap:focus-within {
  border-color: var(--pink);
}

.search-input-wrap .search-icon {
  width: 18px;
  height: 18px;
  opacity: 0.4;
  flex-shrink: 0;
}

.search-input-wrap input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: var(--text);
}

.search-input-wrap input::placeholder {
  color: var(--text-dim);
}

.search-btn {
  padding: 10px 28px;
  background: var(--pink);
  color: white;
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
