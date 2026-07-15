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
                <div class="author-meta">
                  <span class="author-name">{{ post.authorName || '匿名用户' }}</span>
                  <span class="post-time">{{ formatDateTime(post.createTime) }}</span>
                </div>
              </div>
            </div>

            <h1 class="post-title">{{ post.title }}</h1>

            <div class="post-images" v-if="post.images && post.images.length > 0">
              <div class="carousel-container">
                <div class="carousel-track" :style="{ transform: `translateX(-${currentImage * 100}%)` }">
                  <img v-for="(img, index) in post.images" :key="index" :src="img" :alt="`图片${index + 1}`" class="carousel-slide" />
                </div>

                <!-- 左右箭头 -->
                <button v-if="post.images.length > 1" class="carousel-arrow carousel-prev" @click="prevImage">
                  <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
                </button>
                <button v-if="post.images.length > 1" class="carousel-arrow carousel-next" @click="nextImage">
                  <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 6 15 12 9 18"/></svg>
                </button>

                <!-- 底部指示点 -->
                <div class="carousel-dots" v-if="post.images.length > 1">
                  <span v-for="(_, index) in post.images" :key="index" class="carousel-dot" :class="{ active: index === currentImage }" @click="currentImage = index"></span>
                </div>
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
            <button class="action-btn" :class="{ active: liked }" @click="togglePostLike">
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
              <span class="action-count">{{ commentTotal }}</span>
            </button>
          </div>

          <div class="comment-section" ref="commentSection">
            <div class="section-header">
              <h2>评论 ({{ commentTotal }})</h2>
            </div>

            <!-- 顶部评论框：始终显示，用于回复帖子 -->
            <div class="comment-input-area">
              <div class="input-row">
                <textarea
                  v-model="commentInput"
                  class="comment-input"
                  placeholder="写下你的评论..."
                  rows="3"
                ></textarea>
                <button class="send-btn" :disabled="!commentInput.trim()" @click="submitTopComment">
                  发送
                </button>
              </div>
            </div>

            <div class="comment-list" v-if="comments.length > 0">
              <template v-for="comment in comments" :key="comment.id">
                <CommentItem
                  :comment="comment"
                  :is-top-level="true"
                  :current-user-id="currentUserId"
                  @reply="handleReply"
                  @deleted="handleCommentDeleted"
                />
                <!-- 回复框：在该顶级评论下方显示 -->
                <div v-if="activeReplyId === comment.id" class="inline-reply-box">
                  <div class="reply-target-bar">
                    <span>回复 @{{ replyTarget?.comment.userName || '匿名用户' }}</span>
                    <button class="cancel-reply-btn" @click="cancelReply">取消</button>
                  </div>
                  <div class="input-row">
                    <textarea
                      v-model="commentInput"
                      class="comment-input"
                      :placeholder="`回复 @${replyTarget?.comment.userName || '匿名用户'}...`"
                      rows="2"
                      ref="inlineReplyInput"
                    ></textarea>
                    <button class="send-btn" :disabled="!commentInput.trim()" @click="submitComment">
                      发送
                    </button>
                  </div>
                </div>
              </template>

              <!-- 加载更多评论 -->
              <div v-if="!commentFinished" class="load-more-wrap">
                <button
                  class="load-more-btn"
                  :disabled="commentLoading"
                  @click="loadMoreComments"
                >
                  {{ commentLoading ? '加载中...' : '加载更多评论' }}
                </button>
              </div>

              <!-- 没有更多评论 -->
              <div v-else-if="comments.length > 0" class="no-more-wrap">
                <p class="no-more-text">没有更多评论了</p>
              </div>
            </div>

            <div class="empty-comments" v-else-if="!commentLoading">
              <p>暂无评论，来发表第一条评论吧~</p>
            </div>

            <div class="empty-comments" v-else>
              <p>加载中...</p>
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPostDetail, togglePostLike, togglePostCollect, recordPostView } from '@/api/post'
import {
  getCommentList,
  createComment,
} from '@/api/comment'
import type { PostVO } from '@/api/post'
import type { CommentVO, CreateCommentRequest } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import CommentItem from '@/components/CommentItem.vue'
import likeIcon from '@/assets/icons/like.svg'
import likeOutlineIcon from '@/assets/icons/like-outline.svg'
import favoriteIcon from '@/assets/icons/favorite.svg'
import favoriteOutlineIcon from '@/assets/icons/favorite-outline.svg'
import shareIcon from '@/assets/icons/share.svg'
import commentIcon from '@/assets/icons/comment.svg'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const post = ref<PostVO | null>(null)
const loading = ref(false)
const liked = ref(false)
const collected = ref(false)
const commentInput = ref('')
const commentSection = ref<HTMLElement | null>(null)
const currentImage = ref(0)
const replyTarget = ref<{ comment: CommentVO; parentId: string } | null>(null)
const activeReplyId = ref<string | null>(null) // 哪个顶级评论下方显示回复框

// ===== 评论列表 =====
const comments = ref<CommentVO[]>([])
const commentPage = ref(1)
const commentTotal = ref(0)
const commentSize = 20
const commentLoading = ref(false)
const commentFinished = ref(false)

const currentUserId = computed<string | null>(() => {
  const info = userStore.userInfo
  if (info?.id) return String(info.id)
  if (info?.userId) return String(info.userId)
  return null
})

const contentParagraphs = computed(() => {
  if (!post.value?.content) return []
  return post.value.content.split('\n').filter(p => p.trim())
})

const prevImage = () => {
  if (currentImage.value > 0) currentImage.value--
}

const nextImage = () => {
  if (post.value?.images && currentImage.value < post.value.images.length - 1) {
    currentImage.value++
  }
}

const formatNumber = (num: number) => {
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
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

const formatDateTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}年${m}月${day}日 ${h}:${min}`
}

// ===== 帖子 =====
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
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const togglePostLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!post.value) return

  const oldLiked = liked.value
  const oldLikes = post.value.likes

  liked.value = !oldLiked
  post.value.likes = oldLiked ? Math.max(oldLikes - 1, 0) : oldLikes + 1

  try {
    const res = await togglePostLike(String(post.value.id))
    if (res.data.code === 200) {
      post.value.likes = res.data.data
    }
  } catch {
    liked.value = oldLiked
    post.value.likes = oldLikes
  }
}

const toggleCollect = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!post.value) return

  const oldCollected = collected.value
  const oldCollections = post.value.collections

  collected.value = !oldCollected
  post.value.collections = oldCollected ? Math.max(oldCollections - 1, 0) : oldCollections + 1

  try {
    const res = await togglePostCollect(String(post.value.id))
    if (res.data.code === 200) {
      post.value.collections = res.data.data
    }
  } catch {
    collected.value = oldCollected
    post.value.collections = oldCollections
  }
}

const handleShare = () => {
  ElMessage.success('分享链接已复制')
}

const scrollToComment = () => {
  commentSection.value?.scrollIntoView({ behavior: 'smooth' })
}

// ===== 评论列表加载 =====
const loadComments = async (reset = false) => {
  if (!post.value) return
  if (commentLoading.value) return

  if (reset) {
    commentPage.value = 1
    commentFinished.value = false
    comments.value = []
  }

  if (commentFinished.value) return

  commentLoading.value = true
  try {
    const res = await getCommentList({
      postId: String(post.value.id),
      page: commentPage.value,
      size: commentSize,
    })
    const pageResult = res.data?.data
    if (pageResult) {
      const records = pageResult.records || []
      if (reset) {
        comments.value = records
      } else {
        comments.value.push(...records)
      }
      commentTotal.value = pageResult.total || 0
      if (comments.value.length >= commentTotal.value) {
        commentFinished.value = true
      }
    }
  } catch {
    ElMessage.error('加载评论失败')
  } finally {
    commentLoading.value = false
  }
}

const loadMoreComments = () => {
  commentPage.value++
  loadComments(false)
}

// ===== 发表评论 / 回复 =====
const submitTopComment = async () => {
  if (!commentInput.value.trim()) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!post.value) return

  try {
    const data: CreateCommentRequest = {
      postId: String(post.value.id),
      content: commentInput.value.trim(),
    }
    const res = await createComment(data)
    if (res.data.code === 200) {
      ElMessage.success('评论成功')
      commentInput.value = ''
      // 后端返回完整 CommentVO，直接插入列表第一条展示
      comments.value.unshift(res.data.data)
      commentTotal.value++
    }
  } catch {
    ElMessage.error('评论失败')
  }
}

const submitComment = async () => {
  if (!commentInput.value.trim()) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!post.value || !replyTarget.value) return

  try {
    const data: CreateCommentRequest = {
      postId: String(post.value.id),
      content: commentInput.value.trim(),
      parentId: replyTarget.value.parentId,
      replyToUserId: replyTarget.value.comment.userId,
    }
    const res = await createComment(data)
    if (res.data.code === 200) {
      ElMessage.success('回复成功')
      commentInput.value = ''
      const replyParentId = replyTarget.value.parentId
      replyTarget.value = null
      activeReplyId.value = null
      // 局部更新：回复数 +1，同时把新回复插入当前展开的 children 列表
      const idx = comments.value.findIndex(c => c.id === replyParentId)
      if (idx !== -1) {
        const updated = {
          ...comments.value[idx],
          totalReplies: (comments.value[idx].totalReplies || 0) + 1,
        }
        // 如果当前父评论的 children 已展开，把新回复追加到末尾
        if (updated.children && updated.children.length > 0) {
          const newReply = res.data.data
          updated.children = [...updated.children, newReply]
        }
        comments.value[idx] = updated
      }
    }
  } catch {
    ElMessage.error('回复失败')
  }
}

const handleReply = (payload: { comment: CommentVO; parentId: string }) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  replyTarget.value = payload
  activeReplyId.value = payload.parentId
  commentInput.value = ''
}

const cancelReply = () => {
  replyTarget.value = null
  activeReplyId.value = null
  commentInput.value = ''
}

const handleCommentDeleted = (commentId: string) => {
  // 从列表中移除该评论
  comments.value = comments.value.filter(c => c.id !== commentId)
  commentTotal.value = Math.max(0, commentTotal.value - 1)
}

const goHome = () => {
  router.push('/')
}

onMounted(async () => {
  await loadPost()
  if (post.value) {
    loadComments(true)
    // 静默上报浏览，失败不影响展示
    recordPostView(String(route.params.id)).catch(() => {})
  }
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
  max-width: 860px;
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
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
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

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
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

.carousel-container {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  overflow: hidden;
  background: #f5f5f5;
}

.carousel-track {
  display: flex;
  transition: transform 0.3s ease;
  height: 100%;
}

.carousel-slide {
  min-width: 100%;
  height: 100%;
  object-fit: cover;
  user-select: none;
  pointer-events: none;
}

.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  z-index: 2;
}

.carousel-arrow:hover {
  background: rgba(0, 0, 0, 0.6);
}

.carousel-prev {
  left: 12px;
}

.carousel-next {
  right: 12px;
}

.carousel-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  z-index: 2;
}

.carousel-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background 0.2s;
}

.carousel-dot.active {
  background: white;
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

/* 评论输入区 */
.comment-input-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

/* 内联回复框 */
.inline-reply-box {
  margin-left: 68px;
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(180, 132, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(180, 132, 255, 0.15);
}

.input-row {
  display: flex;
  gap: 12px;
}

.reply-target-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  margin-bottom: 8px;
  background: rgba(180, 132, 255, 0.08);
  border-radius: 8px;
  font-size: 13px;
  color: var(--purple);
}

.reply-target-bar span {
  font-weight: 500;
}

.cancel-reply-btn {
  background: transparent;
  border: none;
  color: var(--text-dim);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
}

.cancel-reply-btn:hover {
  background: rgba(0, 0, 0, 0.05);
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

/* 评论列表 */
.comment-list {
  display: flex;
  flex-direction: column;
}

/* 加载更多 */
.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}

.load-more-btn {
  padding: 8px 24px;
  border-radius: 20px;
  background: transparent;
  border: 1.5px solid var(--border);
  font-size: 13px;
  color: var(--text-dim);
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.load-more-btn:hover:not(:disabled) {
  border-color: var(--pink);
  color: var(--pink);
}

.load-more-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 没有更多评论 */
.no-more-wrap {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}

.no-more-text {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
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
