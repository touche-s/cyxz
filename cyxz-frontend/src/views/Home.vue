<template>
  <main class="main-content">
    <div class="page-inner">
    <div class="hero-banner">
      <div class="hero-text">
        <div class="hero-eyebrow">
          <span class="pulse-dot"></span>
          今日推荐
        </div>
        <h1>在<em>这里</em>发现<br />属于你的世界</h1>
        <p>每一次分享都是一场奇遇，每一个灵感都值得被看见</p>
        <div class="hero-stats">
          <div class="hero-stat"><b>128K+</b>创作者</div>
          <div class="hero-stat"><b>2.4M+</b>作品</div>
          <div class="hero-stat"><b>580+</b>今日上新</div>
        </div>
      </div>
      <div class="hero-illust">
        <div class="circle c1"></div>
        <div class="circle c2"></div>
        <div class="circle c3"></div>
        <div class="char-emoji"></div>
      </div>
    </div>

    <div class="cat-row">
      <span 
        class="cat-pill" 
        :class="{ active: selectedCategoryId === null }"
        @click="selectCategory(null)"
      >
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
        {{ cat.name }}
      </span>
    </div>

    <div class="section-label">
      精选内容
      <a href="#">查看更多 <span class="arrow-icon">→</span></a>
    </div>

    <div class="content-grid" v-if="!loading">
      <div class="card" v-for="post in posts" :key="post.id" @click="viewPost(post.id)">
        <div class="card-cover">
          <img v-if="post.cover" :src="post.cover" class="img" alt="cover" />
          <div v-else class="img" :style="{ background: getGradient(post.id) }"></div>
          <span class="card-badge" v-if="post.categoryName">{{ post.categoryName }}</span>
          <button class="card-save" @click.stop="toggleSave(post.id)">♡</button>
        </div>
        <div class="card-body">
          <div class="card-title">{{ post.title }}</div>
          <div class="card-meta">
            <div class="card-author">
              <div class="card-avatar" v-if="!post.authorAvatar"></div>
              <img v-else :src="post.authorAvatar" class="card-avatar" alt="avatar" />
              <span class="card-author-name">{{ post.authorName || '匿名用户' }}</span>
            </div>
            <div class="card-stats">
              <span><img src="@/assets/icons/like.svg" alt="like" class="stat-icon" /> {{ formatNumber(post.likes) }}</span>
              <span><img src="@/assets/icons/comment.svg" alt="comment" class="stat-icon" /> {{ post.comments }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-if="!loading && posts.length === 0" class="empty-state">
      <p>暂无内容</p>
    </div>

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
import { useRouter } from 'vue-router'
import { getPostList, getCategoryList } from '@/api/post'
import type { PostVO, CategoryVO } from '@/api/post'

const router = useRouter()

const posts = ref<PostVO[]>([])
const categories = ref<CategoryVO[]>([])
const selectedCategoryId = ref<number | null>(null)
const loading = ref(false)

// 渐变色数组（用于没有封面的帖子）
const gradients = [
  'linear-gradient(135deg, #ffd4e8, #ffa8c8, #ff8db5)',
  'linear-gradient(135deg, #d4e8ff, #a8c8ff, #8db5ff)',
  'linear-gradient(135deg, #f0d4ff, #d8b0ff, #c084fc)',
  'linear-gradient(135deg, #fff0d4, #ffe0a8, #ffd08d)',
  'linear-gradient(135deg, #d4ffe8, #a8ffd0, #8dffb5)',
  'linear-gradient(135deg, #ffe8d4, #ffc8a8, #ffa88d)',
  'linear-gradient(135deg, #e8d4ff, #d0b0ff)',
  'linear-gradient(135deg, #c8ffe8, #a0ffd0)',
]

const getGradient = (id: number) => {
  return gradients[id % gradients.length]
}

const formatNumber = (num: number) => {
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    if (res.data.code === 200) {
      categories.value = res.data.data
    }
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
    const res = await getPostList(params)
    if (res.data.code === 200) {
      posts.value = res.data.data
    }
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

const viewPost = (postId: number) => {
  router.push(`/post/${postId}`)
}

const toggleSave = (postId: number) => {
  console.log('收藏帖子:', postId)
  // TODO: 实现收藏功能
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
  max-width: 1500px;
  margin: 0 auto;
  padding: 0 32px;
}

/* ===== Hero Banner ===== */
.hero-banner {
  position: relative;
  background:
    radial-gradient(circle at 10% 20%, #FFD6E8 0%, transparent 60%),
    radial-gradient(circle at 90% 80%, #E2D9FF 0%, transparent 60%),
    linear-gradient(135deg, #FFD6E8, #E2D9FF);
  border-radius: 20px;
  padding: 48px 44px;
  margin-bottom: 28px;
  overflow: hidden;
  border: 1.5px solid rgba(255, 182, 215, 0.25);
  display: flex;
  align-items: center;
  gap: 40px;
  box-shadow: 0 8px 32px rgba(180, 132, 255, 0.1);
}

/* Floating decorations */
.hero-banner::before {
  content: '';
  position: absolute;
  top: 20px;
  right: 10%;
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(255, 138, 200, 0.12), transparent 70%);
  border-radius: 50%;
  animation: decoFloat 5s ease-in-out infinite;
}

.hero-banner::after {
  content: '';
  position: absolute;
  bottom: 30px;
  left: 60%;
  width: 60px;
  height: 60px;
  background: radial-gradient(circle, rgba(180, 132, 255, 0.1), transparent 70%);
  border-radius: 50%;
  animation: decoFloat 4s ease-in-out infinite 1s;
}

@keyframes decoFloat {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-10px) scale(1.1); }
}

.hero-text { flex: 1; z-index: 1; }

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 20px;
  background: rgba(255, 138, 200, 0.12);
  border: 1.5px solid rgba(255, 138, 200, 0.25);
  font-size: 11px;
  font-weight: 700;
  color: var(--pink);
  margin-bottom: 18px;
  letter-spacing: 2px;
  transition: all 0.22s ease-out;
}
.hero-eyebrow:hover {
  transform: translateY(-2px);
  background: rgba(255, 138, 200, 0.18);
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 107, 157, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(255, 107, 157, 0); }
}

.hero-text h1 {
  font-size: 40px;
  font-weight: 900;
  line-height: 1.25;
  margin-bottom: 14px;
  color: var(--text);
  text-shadow: 0 2px 12px rgba(255, 107, 157, 0.08);
}

.hero-text h1 em {
  font-style: normal;
  background: linear-gradient(135deg, #FF8AC8, #B484FF);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 900;
}

.hero-text p {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.7;
  max-width: 400px;
  opacity: 0.85;
}

/* Stats with dividers */
.hero-stats {
  display: flex;
  gap: 0;
  margin-top: 24px;
  align-items: center;
}

.hero-stat {
  font-size: 12px;
  color: var(--text-dim);
  padding: 8px 24px;
  border-radius: 12px;
  transition: all 0.22s ease-out;
  cursor: default;
}
.hero-stat:hover {
  background: rgba(255, 138, 200, 0.06);
}
.hero-stat + .hero-stat {
  border-left: 1.5px solid rgba(180, 132, 255, 0.15);
}

.hero-stat b {
  display: block;
  font-size: 22px;
  font-weight: 900;
  background: linear-gradient(135deg, #FF8AC8, #B484FF);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 2px;
}

.hero-illust {
  flex-shrink: 0;
  z-index: 1;
  width: 280px;
  height: 200px;
  position: relative;
}

.circle {
  position: absolute;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  opacity: 0.15;
  animation: circleFloat 4s ease-in-out infinite;
}

.c1 { background: var(--pink); top: 10px; left: 40px; }
.c2 { background: var(--purple); bottom: 0; right: 30px; animation-delay: 1s; }
.c3 { background: var(--blue); top: 60px; right: 0; width: 80px; height: 80px; animation-delay: 2s; }

.char-emoji {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 80px;
  animation: charBounce 3s ease-in-out infinite;
  filter: drop-shadow(0 4px 12px rgba(255, 107, 157, 0.3));
}

.char-emoji::before { content: ''; }

@keyframes circleFloat {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-12px) scale(1.05); }
}

@keyframes charBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

/* ===== Category Tabs ===== */
.cat-row {
  display: flex;
  gap: 8px;
  margin-bottom: 28px;
  overflow-x: auto;
  padding: 4px 4px;
}

.cat-row::-webkit-scrollbar { display: none; }

.cat-pill {
  padding: 9px 20px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  border: 1.5px solid rgba(255, 138, 200, 0.2);
  background: white;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
  flex-shrink: 0;
}

.cat-pill.active {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  border-color: transparent;
  box-shadow:
    0 4px 16px rgba(255, 107, 157, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.cat-pill:hover:not(.active) {
  border-color: rgba(255, 138, 200, 0.4);
  color: #B484FF;
  background: rgba(180, 132, 255, 0.04);
  transform: translateY(-1px);
}

.cat-icon {
  width: 14px;
  height: 14px;
  margin-right: 4px;
}

.cat-pill.active .cat-icon {
  filter: brightness(0) invert(1);
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
  color: #B484FF;
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
  background: white;
  border-radius: 16px;
  overflow: hidden;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.08);
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
  font-size: 16px;
  cursor: pointer;
  border: none;
  transition: all 0.22s ease-out;
  color: var(--text-dim);
}

.card-save:hover {
  color: var(--pink);
  background: white;
  transform: scale(1.15);
  box-shadow: 0 2px 12px rgba(255, 107, 157, 0.2);
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

.card-stats { display: flex; gap: 12px; font-size: 12px; color: var(--text-dim); }
.card-stats span { display: flex; align-items: center; gap: 3px; }
.card-stats .stat-icon { width: 14px; height: 14px; }

/* ===== Loading & Empty States ===== */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 107, 157, 0.2);
  border-top-color: var(--pink);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-container p {
  font-size: 14px;
  color: var(--text-dim);
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-state p {
  font-size: 16px;
  color: var(--text-dim);
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

.footer-copy { font-size: 11px; color: #c4a0b8; }

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .content-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 900px) {
  .content-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .hero-banner { flex-direction: column; padding: 32px 24px; gap: 24px; }
  .hero-illust { width: 200px; height: 160px; }
  .hero-text h1 { font-size: 28px; }
  .main-content { padding: 90px 0 60px; }
  .content-grid { grid-template-columns: 1fr; }
  .hero-stats { flex-wrap: wrap; }
  .hero-stat + .hero-stat { border-left: none; }
}
</style>
