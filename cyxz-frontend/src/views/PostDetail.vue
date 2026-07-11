<template>
  <main class="main-content">
    <div class="page-inner">
      <button class="back-btn" @click="goBack">← 返回</button>

      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="post" class="post-detail">
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
          <div v-for="(img, index) in post.images" :key="index" class="post-image-item">
            <img :src="img" :alt="`图片${index + 1}`" />
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

        <div class="post-actions">
          <div class="action-group">
            <button class="action-btn" :class="{ active: liked }" @click="toggleLike">
              <img :src="liked ? likeIcon : likeOutlineIcon" alt="like" class="action-icon" />
              <span class="action-count">{{ formatNumber(post.likes) }}</span>
            </button>
          </div>
          <div class="action-group">
            <button class="action-btn" :class="{ active: collected }" @click="toggleCollect">
              <img :src="collected ? favoriteIcon : favoriteOutlineIcon" alt="favorite" class="action-icon" />
              <span class="action-count">{{ formatNumber(post.collections) }}</span>
            </button>
          </div>
          <div class="action-group">
            <button class="action-btn" @click="handleShare">
              <img :src="shareIcon" alt="share" class="action-icon" />
              <span class="action-count">分享</span>
            </button>
          </div>
          <div class="action-group">
            <button class="action-btn" @click="scrollToComment">
              <img :src="commentIcon" alt="comment" class="action-icon" />
              <span class="action-count">{{ post.comments }}</span>
            </button>
          </div>
        </div>

        <div class="divider"></div>

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
                  <button class="comment-action-btn"><img src="@/assets/icons/like.svg" alt="like" class="action-icon" />{{ comment.likes }}</button>
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
        <button class="back-btn" @click="goBack">返回首页</button>
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPostDetail } from '@/api/post'
import type { PostVO } from '@/api/post'
import likeIcon from '@/assets/icons/like.svg'
import likeOutlineIcon from '@/assets/icons/like-outline.svg'
import favoriteIcon from '@/assets/icons/favorite.svg'
import favoriteOutlineIcon from '@/assets/icons/favorite-outline.svg'
import shareIcon from '@/assets/icons/share.svg'
import commentIcon from '@/assets/icons/comment.svg'

const router = useRouter()
const route = useRoute()

const post = ref<PostVO | null>(null)
const loading = ref(false)
const liked = ref(false)
const collected = ref(false)
const commentInput = ref('')
const commentSection = ref<HTMLElement | null>(null)

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

const goBack = () => {
  router.back()
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

const handleShare = async () => {
  const url = window.location.href
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('链接已复制到剪贴板')
  } catch (err) {
    ElMessage.success('链接已复制')
  }
}

const scrollToComment = () => {
  commentSection.value?.scrollIntoView({ behavior: 'smooth' })
}

const submitComment = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    return
  }

  if (!commentInput.value.trim()) return

  ElMessage.success('评论功能开发中')
  commentInput.value = ''
}

onMounted(() => {
  loadPost()
})
</script>

<style scoped>
.main-content {
  padding: 90px 0 60px;
  min-height: 100vh;
  background: linear-gradient(180deg, #fff5f9 0%, #f8f9ff 100%);
}

.page-inner {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border-radius: 10px;
  background: rgba(255, 107, 157, 0.08);
  color: var(--pink);
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  margin-bottom: 16px;
}

.back-btn:hover {
  background: rgba(255, 107, 157, 0.15);
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
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

.post-detail {
  background: white;
  border-radius: 20px;
  padding: 32px;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 16px rgba(180, 132, 255, 0.06);
}

.post-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.post-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
  line-height: 1.4;
  margin-bottom: 20px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.15);
}

.author-avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.15);
}

.author-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.post-time {
  font-size: 13px;
  color: var(--text-dim);
}

.post-content {
  margin-bottom: 24px;
}

.content-paragraph {
  font-size: 16px;
  line-height: 1.8;
  color: var(--text);
  margin-bottom: 16px;
  text-indent: 2em;
}

.content-paragraph:last-child {
  margin-bottom: 0;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.post-image-item {
  border-radius: 12px;
  overflow: hidden;
}

.post-image-item img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}

.tag-item {
  padding: 6px 14px;
  border-radius: 20px;
  background: rgba(180, 132, 255, 0.08);
  color: var(--purple);
  font-size: 13px;
  font-weight: 500;
}

.post-actions {
  display: flex;
  justify-content: center;
  gap: 32px;
  padding: 20px 0;
  border-bottom: 1px solid var(--border);
  margin-bottom: 24px;
}

.action-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 24px;
  border-radius: 12px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.action-btn:hover {
  background: rgba(255, 107, 157, 0.05);
}

.action-btn.active {
  color: var(--pink);
}

.action-icon {
  font-size: 24px;
  width: 16px;
  height: 16px;
}



.action-count {
  font-size: 13px;
  color: var(--text-dim);
}

.action-btn.active .action-count {
  color: var(--pink);
}

.divider {
  height: 8px;
  background: linear-gradient(180deg, var(--border) 0%, transparent 100%);
  margin: 0 -32px;
}

.comment-section {
  margin-top: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}

.comment-input-area {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.comment-input {
  flex: 1;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1.5px solid var(--border);
  font-size: 14px;
  color: var(--text);
  resize: none;
  transition: all 0.22s ease-out;
}

.comment-input:focus {
  outline: none;
  border-color: var(--pink);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.1);
}

.comment-input::placeholder {
  color: var(--text-dim);
}

.send-btn {
  align-self: flex-end;
  padding: 12px 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
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
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 107, 157, 0.03);
}

.comment-avatar {
  width: 40px;
  height: 40px;
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
  gap: 12px;
  margin-bottom: 8px;
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
  color: var(--text);
  margin-bottom: 8px;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.comment-action-btn {
  font-size: 12px;
  color: var(--text-dim);
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.22s ease-out;
}

.comment-action-btn:hover {
  color: var(--pink);
}

.empty-comments {
  text-align: center;
  padding: 40px 20px;
}

.empty-comments p {
  font-size: 14px;
  color: var(--text-dim);
}

.empty-state {
  text-align: center;
  padding: 100px 20px;
}

.empty-state p {
  font-size: 16px;
  color: var(--text-dim);
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .page-inner {
    padding: 0 16px;
  }

  .post-detail {
    padding: 20px;
  }

  .post-title {
    font-size: 22px;
  }

  .post-images {
    grid-template-columns: 1fr;
  }

  .post-actions {
    gap: 20px;
  }

  .action-btn {
    padding: 8px 16px;
  }

  .comment-input-area {
    flex-direction: column;
  }

  .send-btn {
    align-self: flex-end;
  }
}
</style>