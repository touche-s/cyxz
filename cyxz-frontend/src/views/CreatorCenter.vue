<template>
  <div class="creator-container">
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo-wrapper">
          <div class="logo-icon"></div>
          <span class="logo-text">次元小站</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-section">
          <button class="nav-item active">
            <span class="nav-icon">🏠</span>
            <span class="nav-text">创作首页</span>
          </button>
        </div>

        <div class="nav-section">
          <div class="section-title">内容管理</div>
          <button class="nav-item">
            <span class="nav-icon">📝</span>
            <span class="nav-text">内容管理</span>
          </button>
          <button class="nav-item">
            <span class="nav-icon">📊</span>
            <span class="nav-text">数据中心</span>
          </button>
        </div>

        <div class="nav-section">
          <div class="section-title">粉丝管理</div>
          <button class="nav-item">
            <span class="nav-icon">👥</span>
            <span class="nav-text">粉丝管理</span>
          </button>
          <button class="nav-item">
            <span class="nav-icon">💬</span>
            <span class="nav-text">互动管理</span>
          </button>
        </div>

        <div class="nav-section">
          <div class="section-title">创作设置</div>
          <button class="nav-item">
            <span class="nav-icon">🎨</span>
            <span class="nav-text">头像</span>
          </button>
          <button class="nav-item">
            <span class="nav-icon">⚙️</span>
            <span class="nav-text">创作设置</span>
          </button>
        </div>

        <div class="nav-section">
          <button class="nav-item">
            <span class="nav-icon">🌸</span>
            <span class="nav-text">花生</span>
          </button>
        </div>
      </nav>
    </aside>

    <main class="main-content">
      <header class="page-header">
        <div class="header-left">
          <h1>创作中心</h1>
          <p>管理你的作品，查看数据表现</p>
        </div>
        <button class="publish-btn" @click="goCreate">
          <span class="btn-icon">✏️</span>
          <span>发布帖子</span>
        </button>
      </header>

      <div class="banner-section">
        <div class="banner">
          <div class="banner-content">
            <h2>记录你的灵感瞬间</h2>
            <p>发布图文作品，和同好一起交流</p>
          </div>
        </div>
      </div>

      <div class="stats-section">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon-wrapper works-icon">
              <span class="stat-icon">📝</span>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalPosts }}</span>
              <span class="stat-label">总作品</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon-wrapper views-icon">
              <span class="stat-icon">👁️</span>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ formatNumber(stats.totalViews) }}</span>
              <span class="stat-label">总浏览</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon-wrapper likes-icon">
              <span class="stat-icon">❤️</span>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ formatNumber(stats.totalLikes) }}</span>
              <span class="stat-label">总点赞</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon-wrapper collections-icon">
              <span class="stat-icon">⭐</span>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ formatNumber(stats.totalCollections) }}</span>
              <span class="stat-label">总收藏</span>
            </div>
          </div>
        </div>
      </div>

      <div class="posts-section">
        <div class="tabs-bar">
          <button 
            v-for="tab in tabs" 
            :key="tab.value"
            class="tab-btn"
            :class="{ active: activeTab === tab.value }"
            @click="switchTab(tab.value)"
          >
            {{ tab.label }}
            <span class="tab-count">{{ tab.count }}</span>
          </button>
        </div>

        <div class="posts-container" v-if="!loading && filteredPosts.length > 0">
          <div class="post-card" v-for="post in filteredPosts" :key="post.id">
            <div class="card-cover" @click="viewPost(post.id)">
              <img v-if="post.cover" :src="post.cover" alt="cover" />
              <div v-else class="cover-placeholder">
                <span>暂无封面</span>
              </div>
            </div>
            <div class="card-content">
              <h3 class="card-title" @click="viewPost(post.id)">{{ post.title }}</h3>
              <div class="card-meta">
                <span class="category-tag" v-if="post.categoryName">{{ post.categoryName }}</span>
                <span class="post-time">{{ formatTime(post.createTime) }}</span>
              </div>
              <div class="card-stats">
                <span>👁️ {{ post.views }}</span>
                <span>❤️ {{ post.likes }}</span>
                <span>💬 {{ post.comments }}</span>
                <span>⭐ {{ post.collections }}</span>
              </div>
            </div>
            <div class="card-status">
              <span class="status-tag" :class="'status-' + post.status">
                {{ statusText(post.status) }}
              </span>
            </div>
            <div class="card-actions">
              <button class="action-btn" @click="editPost(post.id)" title="编辑">
                <span>✏️</span>
              </button>
              <button 
                v-if="post.status === 0" 
                class="action-btn publish" 
                @click="publishPost(post.id)" 
                title="发布"
              >
                <span>🚀</span>
              </button>
              <button 
                v-if="post.status === 2" 
                class="action-btn restore" 
                @click="restorePost(post.id)" 
                title="恢复"
              >
                <span>🔄</span>
              </button>
              <button class="action-btn delete" @click="confirmDelete(post)" title="删除">
                <span>🗑️</span>
              </button>
            </div>
          </div>
        </div>

        <div class="empty-container" v-else-if="!loading">
          <div class="empty-icon">📭</div>
          <p>还没有{{ activeTab === 'published' ? '已发布的' : activeTab === 'draft' ? '草稿' : activeTab === 'deleted' ? '已删除的' : '' }}作品</p>
          <button class="create-btn" @click="goCreate">去创作</button>
        </div>

        <div class="loading-container" v-else>
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>
      </div>

      <div class="status-tabs">
        <button 
          v-for="tab in statusTabs" 
          :key="tab.value"
          class="status-tab-btn"
          :class="{ active: activeTab === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
    </main>

    <div class="modal-overlay" v-if="showDeleteModal" @click="cancelDelete">
      <div class="modal-content" @click.stop>
        <h3>确认删除</h3>
        <p>确定要删除「{{ postToDelete?.title }}」吗？</p>
        <p class="modal-hint">删除后可在"已删除"标签中恢复</p>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="cancelDelete">取消</button>
          <button class="modal-btn confirm" @click="doDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserPosts, deletePost, updatePost } from '@/api/post'
import { useUserStore } from '@/stores/user'
import type { PostVO } from '@/api/post'

const router = useRouter()
const userStore = useUserStore()

const posts = ref<PostVO[]>([])
const loading = ref(false)
const activeTab = ref<'all' | 'published' | 'draft' | 'deleted'>('all')
const showDeleteModal = ref(false)
const postToDelete = ref<PostVO | null>(null)

const stats = computed(() => {
  const activePosts = posts.value.filter(p => p.status !== 2)
  return {
    totalPosts: activePosts.length,
    totalViews: activePosts.reduce((sum, p) => sum + p.views, 0),
    totalLikes: activePosts.reduce((sum, p) => sum + p.likes, 0),
    totalCollections: activePosts.reduce((sum, p) => sum + p.collections, 0),
  }
})

const tabs = computed(() => [
  { label: '总作品', value: 'all' as const, count: posts.value.filter(p => p.status !== 2).length },
  { label: '总浏览', value: 'all' as const, count: stats.value.totalViews },
  { label: '点赞', value: 'all' as const, count: stats.value.totalLikes },
  { label: '已收藏', value: 'all' as const, count: stats.value.totalCollections },
])

const statusTabs = computed(() => [
  { label: '全部', value: 'all' as const },
  { label: '已发布', value: 'published' as const },
  { label: '草稿', value: 'draft' as const },
  { label: '已删除', value: 'deleted' as const },
])

const filteredPosts = computed(() => {
  switch (activeTab.value) {
    case 'published':
      return posts.value.filter(p => p.status === 1)
    case 'draft':
      return posts.value.filter(p => p.status === 0)
    case 'deleted':
      return posts.value.filter(p => p.status === 2)
    default:
      return posts.value.filter(p => p.status !== 2)
  }
})

const statusText = (status: number) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已发布'
    case 2: return '已删除'
    default: return '未知'
  }
}

const formatNumber = (num: number) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

const loadPosts = async () => {
  if (!userStore.userInfo?.id) return
  loading.value = true
  try {
    const res = await getUserPosts(userStore.userInfo.id, { page: 1, size: 100 })
    if (res.data.code === 200) {
      posts.value = res.data.data
    }
  } catch (error) {
    console.error('加载帖子失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const switchTab = (tab: 'all' | 'published' | 'draft' | 'deleted') => {
  activeTab.value = tab
}

const goCreate = () => {
  router.push('/post/create')
}

const viewPost = (postId: number) => {
  router.push(`/post/${postId}`)
}

const editPost = (postId: number) => {
  router.push(`/post/edit/${postId}`)
}

const publishPost = async (postId: number) => {
  try {
    await updatePost({ id: postId, status: 1 })
    const post = posts.value.find(p => p.id === postId)
    if (post) post.status = 1
    ElMessage.success('发布成功')
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error('发布失败')
  }
}

const restorePost = async (postId: number) => {
  try {
    await updatePost({ id: postId, status: 0 })
    const post = posts.value.find(p => p.id === postId)
    if (post) post.status = 0
    ElMessage.success('恢复成功')
  } catch (error) {
    console.error('恢复失败:', error)
    ElMessage.error('恢复失败')
  }
}

const confirmDelete = (post: PostVO) => {
  postToDelete.value = post
  showDeleteModal.value = true
}

const cancelDelete = () => {
  showDeleteModal.value = false
  postToDelete.value = null
}

const doDelete = async () => {
  if (!postToDelete.value) return
  try {
    await deletePost(postToDelete.value.id)
    const post = posts.value.find(p => p.id === postToDelete.value?.id)
    if (post) post.status = 2
    showDeleteModal.value = false
    postToDelete.value = null
    ElMessage.success('删除成功')
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.creator-container {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(180deg, #fff5f9 0%, #f8f9ff 100%);
}

.sidebar {
  width: 220px;
  background: white;
  border-right: 1.5px solid var(--border);
  padding: 20px 0;
  flex-shrink: 0;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  overflow-y: auto;
}

.sidebar-header {
  padding: 0 20px 20px;
  border-bottom: 1px solid var(--border);
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
}

.logo-text {
  font-size: 18px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sidebar-nav {
  padding: 16px 8px;
}

.nav-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-dim);
  padding: 0 12px 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.nav-item {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: all 0.22s ease-out;
}

.nav-item:hover {
  background: rgba(255, 107, 157, 0.05);
  color: var(--pink);
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  font-weight: 600;
}

.nav-icon {
  font-size: 18px;
}

.main-content {
  flex: 1;
  margin-left: 220px;
  padding: 24px 32px;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h1 {
  font-size: 26px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 4px;
}

.header-left p {
  font-size: 14px;
  color: var(--text-dim);
}

.publish-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 25px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

.publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.4);
}

.btn-icon {
  font-size: 16px;
}

.banner-section {
  margin-bottom: 32px;
}

.banner {
  background: linear-gradient(135deg, #ff85a2 0%, #b484ff 100%);
  border-radius: 20px;
  padding: 32px;
  color: white;
  position: relative;
  overflow: hidden;
}

.banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.banner::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.banner-content {
  position: relative;
  z-index: 1;
}

.banner-content h2 {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 8px;
}

.banner-content p {
  font-size: 14px;
  opacity: 0.9;
}

.stats-section {
  margin-bottom: 32px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.06);
  transition: all 0.22s ease-out;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(180, 132, 255, 0.12);
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.works-icon {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(99, 102, 241, 0.1));
}

.views-icon {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(52, 211, 153, 0.1));
}

.likes-icon {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(248, 113, 113, 0.1));
}

.collections-icon {
  background: linear-gradient(135deg, rgba(234, 179, 8, 0.1), rgba(250, 204, 21, 0.1));
}

.stat-icon {
  font-size: 24px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--text);
}

.stat-label {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 2px;
}

.posts-section {
  background: white;
  border-radius: 20px;
  padding: 24px;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.06);
  margin-bottom: 24px;
}

.tabs-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  background: transparent;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
}

.tab-btn.active {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
}

.tab-btn:hover:not(.active) {
  background: rgba(255, 107, 157, 0.05);
  color: var(--pink);
}

.tab-count {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.06);
}

.tab-btn.active .tab-count {
  background: rgba(255, 255, 255, 0.25);
}

.posts-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 107, 157, 0.03);
  border: 1.5px solid transparent;
  transition: all 0.22s ease-out;
}

.post-card:hover {
  border-color: rgba(180, 132, 255, 0.3);
  background: rgba(255, 107, 157, 0.05);
}

.card-cover {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f0e6ff, #ffe6f0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--text-dim);
}

.card-content {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.card-title:hover {
  color: var(--pink);
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.category-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  font-weight: 500;
}

.post-time {
  font-size: 12px;
  color: var(--text-dim);
}

.card-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-dim);
}

.card-status {
  flex-shrink: 0;
}

.status-tag {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 8px;
  font-weight: 500;
}

.status-0 {
  background: #fff3cd;
  color: #856404;
}

.status-1 {
  background: #d4edda;
  color: #155724;
}

.status-2 {
  background: #f8d7da;
  color: #721c24;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.22s ease-out;
  background: white;
}

.action-btn:hover {
  transform: scale(1.1);
}

.action-btn.publish:hover {
  background: #e8f5e9;
}

.action-btn.restore:hover {
  background: #e3f2fd;
}

.action-btn.delete:hover {
  background: #ffebee;
}

.empty-container {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-container p {
  font-size: 16px;
  color: var(--text-dim);
  margin-bottom: 24px;
}

.create-btn {
  padding: 12px 32px;
  border-radius: 25px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  font-size: 15px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.4);
}

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

.status-tabs {
  display: flex;
  gap: 8px;
  justify-content: center;
  padding: 8px;
  background: white;
  border-radius: 14px;
  border: 1.5px solid var(--border);
}

.status-tab-btn {
  padding: 10px 24px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  background: transparent;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
}

.status-tab-btn.active {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
}

.status-tab-btn:hover:not(.active) {
  background: rgba(255, 107, 157, 0.05);
  color: var(--pink);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 20px;
  padding: 32px;
  max-width: 400px;
  width: 90%;
  text-align: center;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-content h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 12px;
}

.modal-content p {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.modal-hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-bottom: 24px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.modal-btn {
  padding: 10px 28px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.22s ease-out;
}

.modal-btn.cancel {
  background: #f3f4f6;
  color: var(--text-dim);
}

.modal-btn.cancel:hover {
  background: #e5e7eb;
}

.modal-btn.confirm {
  background: linear-gradient(135deg, #ff4757, #ff6b81);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 71, 87, 0.3);
}

.modal-btn.confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 71, 87, 0.4);
}

@media (max-width: 1024px) {
  .sidebar {
    width: 60px;
  }
  
  .sidebar-header {
    padding: 0;
    display: flex;
    justify-content: center;
  }
  
  .logo-wrapper {
    flex-direction: column;
    gap: 4px;
  }
  
  .logo-text {
    font-size: 10px;
  }
  
  .nav-text {
    display: none;
  }
  
  .section-title {
    display: none;
  }
  
  .nav-item {
    justify-content: center;
    padding: 12px;
  }
  
  .main-content {
    margin-left: 60px;
    padding: 20px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
  
  .main-content {
    margin-left: 0;
    padding: 80px 16px 20px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .banner {
    padding: 20px;
  }
  
  .banner-content h2 {
    font-size: 20px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .post-card {
    flex-wrap: wrap;
  }
  
  .card-cover {
    width: 60px;
    height: 60px;
  }
  
  .card-status {
    width: 100%;
    margin-top: 8px;
  }
  
  .card-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: 8px;
  }
  
  .status-tabs {
    flex-wrap: wrap;
  }
  
  .status-tab-btn {
    flex: 1 1 calc(50% - 4px);
  }
}
</style>