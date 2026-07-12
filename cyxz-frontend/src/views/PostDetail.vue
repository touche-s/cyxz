<template>
  <div class="post-detail-page">
    <div class="background-image"></div>

    <main class="main-content">
      <div class="page-inner">
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="post" class="post-detail">
          <div class="post-main-card">
            <div class="post-header">
              <div class="author-info">
                <img v-if="post.authorAvatar" :src="post.authorAvatar" class="author-avatar" />
                <div v-else class="author-avatar-placeholder"></div>
                <span class="author-name">{{ post.authorName || '匿名用户' }}</span>
              </div>
              <span class="post-time">{{ formatTime(post.createTime) }}</span>
            </div>

            <h1 class="post-title">{{ post.title }}</h1>

            <div class="post-images" v-if="post.images && post.images.length > 0">
              <div class="image-wrapper">
                <img v-for="(img, index) in post.images" :key="index" :src="img" :alt="`图片${index + 1}`" />
              </div>
              <div class="image-indicator" v-if="post.images.length > 1">
                {{ currentImage + 1 }}/{{ post.images.length }}
              </div>
            </div>

            <div class="post-content">
              <p v-for="(paragraph, index) in contentParagraphs" :key="index" class="content-paragraph">
                {{ paragraph }}
              </p>
            </div>

            <div class="post-tags" v-if="post.tags && post.tags.length > 0">
              <span v-for="tag in post.tags" :key="tag" class="tag-item">#{{ tag }}</span>
            </div>
          </div>

          <div class="post-action-bar">
            <button class="action-btn" :class="{ active: liked }" @click="toggleLike">
              <img :src="liked ? likeIcon : likeOutlineIcon" alt="like" class="action-icon" />
              <span class="action-count">{{ formatNumber(post.likes) }}</span>
            </button>
            <button class="action-btn" :class="{ active: collected }" @click="toggleCollect">
              <img :src="collected ? favoriteIcon : favoriteOutlineIcon" alt="favorite" class="action-icon" />
              <span class="action-count">{{ formatNumber(post.collections) }}</span>
            </button>
            <button class="action-btn" @click="handleShare">
              <img :src="shareIcon" alt="share" class="action-icon" />
              <span class="action-count">分享</span>
            </button>
            <button class="action-btn" @click="scrollToComment">
              <img :src="commentIcon" alt="comment" class="action-icon" />
              <span class="action-count">{{ post.comments }}</span>
            </button>
          </div>

          <div class="comment-section" ref="commentSection">
            <div class="section-header">
              <h2>评论 ({{ post.comments }})</h2>
            </div>

            <div class="comment-input-area">
              <textarea
                v-model="commentInput"
                class="comment-input"
                placeholder="写下你的评论..."
                rows="3"
              ></textarea>
              <button class="send-btn" :disabled="!commentInput.trim()" @click="submitComment">
                发送
              </button>
            </div>

            <div class="comment-list" v-if="comments.length > 0">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <div class="comment-avatar"></div>
                <div class="comment-content">
                  <div class="comment-header">
                    <span class="comment-author">{{ comment.authorName }}</span>
                    <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  </div>
                  <p class="comment-text">{{ comment.content }}</p>
                  <div class="comment-actions">
                    <button class="comment-action-btn">
                      <img :src="liked ? likeIcon : likeOutlineIcon" alt="like" class="action-icon" />
                      {{ comment.likes }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div class="empty-comments" v-else>
              <p>暂无评论，来发表第一条评论吧~</p>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <p>帖子不存在或已删除</p>
          <button class="back-btn" @click="goHome">返回首页</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPostDetail } from '@/api/post'
import type { PostVO } from '@/api/post'
import likeIcon from '@/assets/icons/like.svg'
import likeOutlineIcon from '@/assets/icons/like-outline.svg'
import favoriteIcon from '@/assets/icons/favorite.svg'
import favoriteOutlineIcon from '@/assets/icons/favorite-outline.svg'
import shareIcon from '@/assets/icons/share.svg'
import commentIcon from '@/assets/icons/comment.svg'

const route = useRoute()
const router = useRouter()

const post = ref<PostVO | null>(null)
const loading = ref(false)
const liked = ref(false)
const collected = ref(false)
const commentInput = ref('')
const commentSection = ref<HTMLElement | null>(null)
const currentImage = ref(0)

const comments = ref([
  {
    id: 1,
    authorName: '用户A',
    content: '这个帖子太棒了！内容很丰富~',
    likes: 12,
    createTime: '2026-07-09 15:30:00',
  },
  {
    id: 2,
    authorName: '用户B',
    content: '学到了很多东西，感谢分享！',
    likes: 8,
    createTime: '2026-07-09 16:45:00',
  },
])

const contentParagraphs = computed(() => {
  if (!post.value?.content) return []
  return post.value.content.split('\n').filter(p => p.trim())
})

const formatNumber = (num: number) => {
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

const loadPost = async () => {
  const postId = String(route.params.id)
  loading.value = true
  try {
    const res = await getPostDetail(postId)
    if (res.data.code === 200) {
      post.value = res.data.data as PostVO
      liked.value = post.value.liked || false
      collected.value = post.value.collected || false
    }
  } catch (error) {
    console.error('加载帖子详情失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const toggleLike = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    return
  }

  liked.value = !liked.value
  if (post.value) {
    post.value.likes += liked.value ? 1 : -1
  }
}

const toggleCollect = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    return
  }

  collected.value = !collected.value
  if (post.value) {
    post.value.collections += collected.value ? 1 : -1
  }
}

const handleShare = () => {
  ElMessage.success('分享链接已复制')
}

const scrollToComment = () => {
  commentSection.value?.scrollIntoView({ behavior: 'smooth' })
}

const submitComment = () => {
  if (!commentInput.value.trim()) return
  ElMessage.success('评论成功')
  commentInput.value = ''
}

const goHome = () => {
  router.push('/')
}

onMounted(() => {
  loadPost()
})
</script>

<style scoped>
.post-detail-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.background-image {
  position: fixed;
  inset: 0;
  background-image: url('@/assets/images/post-detail-bg.svg');
  background-size: cover;
  background-position: center;
  pointer-events: none;
  z-index: 0;
}

.main-content {
  position: relative;
  z-index: 1;
  padding-top: 80px;
}

.page-inner {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 20px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid rgba(255, 107, 157, 0.2);
  border-top-color: var(--pink);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-container p {
  margin-top: 16px;
  font-size: 14px;
  color: var(--text-dim);
}

.post-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-main-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 30px rgba(255, 107, 157, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.post-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid white;
  box-shadow: 0 2px 12px rgba(180, 132, 255, 0.25);
}

.author-avatar-placeholder {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: 2px solid white;
  box-shadow: 0 2px 10px rgba(180, 132, 255, 0.2);
}

.author-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.post-time {
  font-size: 13px;
  color: var(--text-dim);
}

.post-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  line-height: 1.5;
  margin-bottom: 20px;
}

.post-images {
  position: relative;
  margin-bottom: 20px;
  border-radius: 16px;
  overflow: hidden;
}

.image-wrapper {
  width: 100%;
  aspect-ratio: 16/9;
  background: #f5f5f5;
}

.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-indicator {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.post-content {
  margin-bottom: 16px;
}

.content-paragraph {
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.content-paragraph:last-child {
  margin-bottom: 0;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  padding: 6px 14px;
  border-radius: 16px;
  background: rgba(180, 132, 255, 0.1);
  color: var(--purple);
  font-size: 13px;
  font-weight: 500;
}

.post-action-bar {
  display: flex;
  justify-content: space-around;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 10px 20px;
  box-shadow: 0 4px 30px rgba(255, 107, 157, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  padding: 8px 20px;
  border-radius: 12px;
}

.action-btn:hover {
  background: rgba(255, 107, 157, 0.08);
}

.action-btn.active {
  background: rgba(255, 107, 157, 0.1);
}

.action-btn img {
  width: 24px;
  height: 24px;
  transition: transform 0.22s ease-out;
}

.action-btn:hover img {
  transform: scale(1.1);
}

.action-count {
  font-size: 13px;
  color: var(--text-dim);
  transition: color 0.22s ease-out;
}

.action-btn.active .action-count {
  color: var(--pink);
}

.comment-section {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 14px 20px;
  box-shadow: 0 4px 30px rgba(255, 107, 157, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.section-header {
  margin-bottom: 16px;
}

.section-header h2 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
}

.comment-input-area {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.comment-input {
  flex: 1;
  padding: 8px 12px;
  border-radius: 16px;
  border: 1.5px solid var(--border);
  font-size: 14px;
  resize: none;
  height: 38px;
  outline: none;
  transition: border-color 0.22s ease-out;
  background: rgba(255, 255, 255, 0.6);
}

.comment-input:focus {
  border-color: var(--pink);
  background: white;
}

.comment-input::placeholder {
  color: var(--text-dim);
}

.send-btn {
  padding: 10px 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  white-space: nowrap;
}

.send-btn:hover:not(:disabled) {
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.35);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.comment-author {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.comment-time {
  font-size: 12px;
  color: var(--text-dim);
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.comment-action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--text-dim);
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.22s ease-out;
}

.comment-action-btn:hover {
  background: rgba(255, 107, 157, 0.08);
  color: var(--pink);
}

.comment-action-btn img {
  width: 14px;
  height: 14px;
}

.empty-comments {
  text-align: center;
  padding: 30px 0;
}

.empty-comments p {
  font-size: 14px;
  color: var(--text-dim);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
}

.empty-state p {
  font-size: 16px;
  color: var(--text-dim);
  margin-bottom: 20px;
}

.empty-state .back-btn {
  padding: 10px 32px;
  border-radius: 20px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.empty-state .back-btn:hover {
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.35);
}

@media (max-width: 768px) {
  .page-inner {
    padding: 0 16px;
  }

  .post-main-card {
    padding: 16px;
  }

  .post-title {
    font-size: 18px;
  }

  .post-action-bar {
    padding: 14px 16px;
  }

  .action-btn {
    padding: 6px 12px;
  }

  .action-btn img {
    width: 22px;
    height: 22px;
  }

  .comment-section {
    padding: 16px;
  }
}
</style>
