<template>
  <main class="main-content">
      <div class="page-inner">

    <div class="cat-row">
      <span 
        class="cat-pill" 
        :class="{ active: selectedCategoryId === null }"
        @click="selectCategory(null)"
      >
        <Icon icon="ph:shooting-star" class="cat-icon" />
        推荐
      </span>
      <span 
        v-for="cat in categories" 
        :key="cat.id"
        class="cat-pill"
        :title="cat.description"
        :class="{ active: selectedCategoryId === cat.id }"
        @click="selectCategory(cat.id)"
      >
        <Icon :icon="getCategoryIcon(cat.name)" class="cat-icon" />
        {{ cat.name }}
      </span>
    </div>

    <div class="section-label">
      精选内容
      <a href="#">查看更多 <span class="arrow-icon">→</span></a>
    </div>

    <div class="content-grid" v-if="!loading">
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        :show-collect="true"
        :show-like="true"
        @click="viewPost"
        @like="handlePostLike"
        @collect="toggleSave"
      />
    </div>

    <LoadingSpinner v-else text="加载中..." />

    <EmptyState v-if="!loading && posts.length === 0" title="暂无内容" />

    <footer class="footer">
      <div class="footer-links">
        <a href="#">关于我们</a>
        <a href="#">社区规范</a>
        <a href="#">帮助中心</a>
        <a href="#">意见反馈</a>
      </div>
      <div class="footer-copy">© 2026 次元小站. All rights reserved.</div>
    </footer>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { getPostList, getCategoryList, likePost, unlikePost, collectPost, uncollectPost } from '@/api/post'
import type { PostVO, CategoryVO } from '@/api/post'
import { useNavigate } from '@/composables/useNavigate'
import { useAuth } from '@/composables/useAuth'
import PostCard from '@/components/PostCard.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'

const { open } = useNavigate()
const { requireLogin } = useAuth()

const iconMap: Record<string, string> = {
  '推荐': 'ph:shooting-star',
  '动漫': 'ph:television',
  '游戏': 'ph:game-controller',
  '绘画': 'ph:palette', '插画': 'ph:palette', '画': 'ph:palette',
  'cos': 'ph:t-shirt',
  '漫展': 'ph:ticket',
  '同人': 'ph:book-open', '文': 'ph:book-open', '小说': 'ph:book-open',
  '周边': 'ph:shopping-bag',
  '闲聊': 'ph:chat-circle',
  '资源': 'ph:folder',
}

function getCategoryIcon(name: string): string {
  const n = name.toLowerCase()
  for (const [key, icon] of Object.entries(iconMap)) {
    if (n.includes(key.toLowerCase())) return icon
  }
  return 'ph:folder'
}

const posts = ref<PostVO[]>([])
const categories = ref<CategoryVO[]>([])
const selectedCategoryId = ref<number | null>(null)
const loading = ref(false)

const loadCategories = async () => {
  try {
    categories.value = await getCategoryList()
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadPosts = async () => {
  loading.value = true
  try {
    const params: any = { page: 1, size: 12 }
    if (selectedCategoryId.value !== null) {
      params.categoryId = selectedCategoryId.value
    }
    const data = await getPostList(params)
    posts.value = data.records || []
  } catch (error) {
    console.error('加载帖子失败:', error)
  } finally {
    loading.value = false
  }
}

const selectCategory = (categoryId: number | null) => {
  selectedCategoryId.value = categoryId
  loadPosts()
}

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
  padding: 0;
}

/* ===== Category Tabs ===== */
.cat-row {
  display: inline-flex;
  max-width: 100%;
  gap: 6px;
  margin-bottom: 28px;
  overflow-x: auto;
  padding: 5px;
  background: rgba(255, 244, 250, 0.96);
  border: 1px solid var(--border-light);
  border-radius: 999px;
  box-shadow: 0 4px 14px rgba(255, 107, 157, 0.06);
}

.cat-row::-webkit-scrollbar { display: none; }

.cat-pill {
  display: flex;
  align-items: center;
  padding: 8px 18px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  border: none;
  background: transparent;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
  flex-shrink: 0;
}

.cat-pill.active {
  background: var(--card);
  color: var(--pink);
  font-weight: 700;
  box-shadow:
    0 4px 12px rgba(255, 107, 157, 0.1),
    inset 0 0 0 1px rgba(255, 107, 157, 0.16);
}

.cat-pill:hover:not(.active) {
  color: var(--pink);
  background: var(--pink-bg);
}

.cat-icon {
  width: 15px;
  height: 15px;
  margin-right: 6px;
  flex-shrink: 0;
}

/* ===== Section Label ===== */
.section-label {
  font-size: 17px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-label a {
  font-size: 13px;
  font-weight: 600;
  color: var(--pink);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.22s ease-out;
}
.section-label a:hover {
  color: var(--purple);
}
.section-label a:hover .arrow-icon {
  transform: translateX(4px);
}
.arrow-icon {
  display: inline-block;
  transition: transform 0.22s ease-out;
}

/* ===== Content Grid ===== */
.content-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 32px;
}

.card {
  background: var(--card);
  border-radius: 16px;
  overflow: hidden;
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow);
  transition: all 0.22s ease-out;
  cursor: pointer;
}

.card:hover {
  transform: translateY(-4px) scale(1.02);
  border-color: rgba(180, 132, 255, 0.3);
  box-shadow: 0 12px 36px rgba(180, 132, 255, 0.15);
}

.card-cover {
  position: relative;
  height: 180px;
  overflow: hidden;
}

.card-cover .img {
  width: 100%;
  height: 100%;
  transition: transform 0.4s;
}

.card:hover .card-cover .img { transform: scale(1.05); }

.card-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 10px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 138, 200, 0.9), rgba(180, 132, 255, 0.9));
  font-size: 10px;
  font-weight: 700;
  color: white;
  backdrop-filter: blur(8px);
}

.card-save {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: none;
  transition: all 0.22s ease-out;
  padding: 0;
  line-height: 0;
}

.card-save .collect-icon {
  width: 16px;
  height: 16px;
  display: block;
  color: var(--pink);
}

.card-save:hover {
  background: white;
  transform: scale(1.15);
  box-shadow: 0 2px 12px rgba(180, 132, 255, 0.2);
}

.card-body { padding: 14px 16px; }

.card-title {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
  color: var(--text);
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-author { display: flex; align-items: center; gap: 8px; }

.card-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: 2px solid white;
  box-shadow: 0 1px 4px rgba(180, 132, 255, 0.15);
}

.card-author-name {
  font-size: 12px;
  color: var(--text-dim);
  font-weight: 500;
  transition: color 0.22s ease-out;
}
.card-author-name:hover {
  color: var(--pink);
}

.card-stats { 
  display: flex; 
  align-items: center; 
  gap: 14px; 
  font-size: 12px; 
  color: var(--text-dim);
  line-height: 1;
}

.card-stats span { 
  display: inline-flex; 
  align-items: center; 
  gap: 3px;
  line-height: 1;
  transition: color 0.22s ease-out;
}

.card-stats span:hover {
  color: var(--text);
}

.card-stats .stat-icon { 
  width: 14px; 
  height: 14px; 
  display: block;
  flex-shrink: 0;
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--text-dim);
  padding: 0;
  transition: color 0.22s ease-out;
  line-height: 1;
  font-family: inherit;
}

.like-btn:hover {
  color: var(--pink);
}

.like-btn.active {
  color: var(--pink);
}

/* ===== Footer ===== */
.footer {
  text-align: center;
  padding: 40px 0 20px;
  border-top: 1px solid var(--border);
  margin-top: 20px;
}

.footer-links { display: flex; justify-content: center; gap: 24px; margin-bottom: 12px; }

.footer-links a {
  font-size: 12px;
  color: var(--text-dim);
  text-decoration: none;
}

.footer-links a:hover { color: var(--pink); }

.footer-copy { font-size: 11px; color: var(--text-dim); }

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .content-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 900px) {
  .content-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .main-content { padding: 90px 0 60px; }
  .content-grid { grid-template-columns: 1fr; }
}
</style>
