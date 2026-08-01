<template>
  <div class="article-detail-page">
    <div class="background-image"></div>

    <main class="main-content">
      <div class="page-inner">
        <LoadingSpinner v-if="loading" text="加载中..." />

        <div v-else-if="post" class="article-detail">
          <div class="article-main-card">
            <button class="article-more-btn" @click.stop="showReportMenu = !showReportMenu">
              <Icon icon="ph:dots-three-outline-vertical" />
            </button>
            <div v-if="showReportMenu" class="article-more-dropdown" @click.stop>
              <button class="article-more-item" @click="handleReportFromMenu">
                <Icon icon="ph:warning" class="more-item-icon" />
                举报
              </button>
            </div>

            <div class="article-header">
              <div class="author-info">
                <img :src="avatarUrl(post.authorAvatar)" class="author-avatar clickable" @click="goToAuthor" />
                <div class="author-meta">
                  <div class="author-name-row">
                    <span class="author-name clickable" @click="goToAuthor">{{ post.authorName || '匿名用户' }}</span>
                    <FollowButton v-if="post.userId && String(post.userId) !== String(currentUserId)"
                            :following="following"
                            :loading="followLoading"
                            variant="author"
                            @toggle="toggleFollow" />
                  </div>
                  <span class="article-time">{{ formatDateTime(post.createTime) }}</span>
                </div>
              </div>
            </div>

            <h1 class="article-title">{{ post.title }}</h1>

            <div class="article-content">
              <v-md-preview :text="post.content" />
            </div>

            <div class="article-tags" v-if="post.tags && post.tags.length > 0">
              <span v-for="tag in post.tags" :key="tag" class="tag-item">#{{ tag }}</span>
            </div>
          </div>

          <div class="article-action-bar">
            <button class="action-btn" :class="{ active: post.liked, popping: likeInteraction.popping }" @click="likeInteraction.toggle">
              <Icon icon="ph:heart" class="action-icon pink-icon" v-show="!post.liked" />
              <Icon icon="ph:heart-fill" class="action-icon pink-icon" v-show="post.liked" />
              <span class="action-count">{{ formatNumber(post.likes) }}</span>
            </button>
            <button class="action-btn" :class="{ active: post.collected, popping: collectInteraction.popping }" @click="collectInteraction.toggle">
              <Icon icon="ph:star" class="action-icon pink-icon" v-show="!post.collected" />
              <Icon icon="ph:star-fill" class="action-icon pink-icon" v-show="post.collected" />
              <span class="action-count">{{ formatNumber(post.collections) }}</span>
            </button>
            <button class="action-btn" @click="handleShare">
              <Icon icon="ph:share-fat" class="action-icon pink-icon" />
              <span class="action-count">分享</span>
            </button>
            <button class="action-btn" @click="scrollToComment">
              <Icon icon="ph:chat-circle-text" class="action-icon pink-icon" />
              <span class="action-count">{{ post.comments ?? 0 }}</span>
            </button>
          </div>

          <div class="comment-section" ref="commentSection">
            <div class="section-header">
              <h2>评论 ({{ post.comments ?? 0 }})</h2>
            </div>

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
                    ></textarea>
                    <button class="send-btn" :disabled="!commentInput.trim()" @click="submitComment">
                      发送
                    </button>
                  </div>
                </div>
              </template>

              <div v-if="!commentFinished" class="load-more-wrap">
                <button
                  class="load-more-btn"
                  :disabled="commentLoading"
                  @click="loadMoreComments"
                >
                  {{ commentLoading ? '加载中...' : '加载更多评论' }}
                </button>
              </div>

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

        <EmptyState v-else title="文章不存在或已删除">
          <template #actions>
            <button class="back-btn" @click="goHome">返回首页</button>
          </template>
        </EmptyState>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useNavigate } from '@/composables/useNavigate'
import { ElMessage } from 'element-plus'
import { getPostDetail, likePost, unlikePost, collectPost, uncollectPost, recordPostView } from '@/api/post'
import { isPublished } from '@/utils/postStatus'
import { formatDateTime, formatNumber } from '@/utils/format'
import { avatarUrl } from '@/utils/avatar'
import {
  getCommentList,
  createComment,
} from '@/api/comment'
import type { PostVO } from '@/api/post'
import type { CommentVO, CreateCommentRequest } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { useFollow } from '@/composables/useFollow'
import CommentItem from '@/components/CommentItem.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import FollowButton from '@/components/FollowButton.vue'
import { useToggleInteraction } from '@/composables/useToggleInteraction'
import { Icon } from '@iconify/vue'
import VMdPreview from '@kangc/v-md-editor/lib/preview'
import githubTheme from '@kangc/v-md-editor/lib/theme/github.js'

VMdPreview.use(githubTheme)

const route = useRoute()
const { open, to } = useNavigate()
const userStore = useUserStore()
const { requireLogin } = useAuth()
const { following, followLoading, checkFollowing, toggleFollow: doFollow } = useFollow()

const post = ref<PostVO | null>(null)
const loading = ref(false)
const commentInput = ref('')
const commentSection = ref<HTMLElement | null>(null)

const likeInteraction = useToggleInteraction({
  target: () => post.value,
  likedField: 'liked',
  countField: 'likes',
  likeApi: (id) => likePost(id),
  unlikeApi: (id) => unlikePost(id),
  idGetter: (p) => String(p.id),
})

const collectInteraction = useToggleInteraction({
  target: () => post.value,
  likedField: 'collected',
  countField: 'collections',
  likeApi: (id) => collectPost(id),
  unlikeApi: (id) => uncollectPost(id),
  idGetter: (p) => String(p.id),
})

const replyTarget = ref<{ comment: CommentVO; parentId: string } | null>(null)
const activeReplyId = ref<string | null>(null)

const comments = ref<CommentVO[]>([])
const commentPage = ref(1)
const commentSize = 20
const commentLoading = ref(false)
const commentFinished = ref(false)

const currentUserId = computed<string | null>(() => {
  const info = userStore.userInfo
  if (info?.id) return String(info.id)
  if (info?.userId) return String(info.userId)
  return null
})

function goToAuthor() {
  if (post.value?.userId) {
    open(`/user/${post.value.userId}`)
  }
}

function toggleFollow() {
  if (!post.value?.userId) return
  doFollow(String(post.value.userId))
}

const handleShare = async () => {
  try {
    await navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const scrollToComment = () => {
  commentSection.value?.scrollIntoView({ behavior: 'smooth' })
}

const showReportMenu = ref(false)

function handleReportFromMenu() {
  showReportMenu.value = false
  ElMessage.info('举报功能即将上线。如遇违规内容，请联系管理员。')
}

function onDocClick() {
  showReportMenu.value = false
}

// ===== 帖子加载 =====
const loadPost = async () => {
  const postId = String(route.params.id)
  loading.value = true
  try {
    const data = await getPostDetail(postId)
    post.value = data as PostVO
    if (!isPublished(post.value.status)) {
      post.value = null
      ElMessage.warning('该内容不可查看')
      loading.value = false
      return
    }
    if (post.value.userId && String(post.value.userId) !== String(currentUserId.value)) {
      await checkFollowing(String(post.value.userId))
    }
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
})

onMounted(async () => {
  await loadPost()
  if (post.value) {
    loadComments(true)
    recordPostView(String(route.params.id)).catch(() => {})
  }
})

// ===== 评论 =====
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
    const pageResult = await getCommentList({
      postId: String(post.value.id),
      page: commentPage.value,
      size: commentSize,
    })
    const records = pageResult.records || []
    if (reset) {
      comments.value = records
    } else {
      comments.value.push(...records)
    }
    if (post.value) post.value.comments = pageResult.total || 0
    if (comments.value.length >= (pageResult.total || 0)) {
      commentFinished.value = true
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

const submitTopComment = async () => {
  if (!commentInput.value.trim()) return
  if (!requireLogin()) return
  if (!post.value) return

  try {
    const data: CreateCommentRequest = {
      postId: String(post.value.id),
      content: commentInput.value.trim(),
    }
    const newComment = await createComment(data)
    ElMessage.success('评论成功')
    commentInput.value = ''
    comments.value.unshift(newComment)
    if (post.value) post.value.comments = (post.value.comments ?? 0) + 1
  } catch {
    ElMessage.error('评论失败')
  }
}

const submitComment = async () => {
  if (!commentInput.value.trim()) return
  if (!requireLogin()) return
  if (!post.value || !replyTarget.value) return

  try {
    const data: CreateCommentRequest = {
      postId: String(post.value.id),
      content: commentInput.value.trim(),
      parentId: replyTarget.value.parentId,
      replyToUserId: replyTarget.value.comment.userId,
    }
    const newComment = await createComment(data)
    ElMessage.success('回复成功')
    commentInput.value = ''
    const replyParentId = replyTarget.value.parentId
    replyTarget.value = null
    activeReplyId.value = null
    const idx = comments.value.findIndex(c => c.id === replyParentId)
    if (idx !== -1) {
      const updated = {
        ...comments.value[idx],
        totalReplies: (comments.value[idx].totalReplies || 0) + 1,
      }
      if (updated.children && updated.children.length > 0) {
        updated.children = [...updated.children, newComment]
      }
      comments.value[idx] = updated
    }
  } catch {
    ElMessage.error('回复失败')
  }
}

const handleReply = (payload: { comment: CommentVO; parentId: string }) => {
  if (!requireLogin()) return
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
  const idx = comments.value.findIndex(c => c.id === commentId)
  if (idx !== -1) {
    comments.value.splice(idx, 1)
  }
  if (post.value) post.value.comments = Math.max(0, (post.value.comments ?? 1) - 1)
}

const goHome = () => {
  to('/')
}
</script>

<style scoped>
.article-detail-page {
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

.article-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-main-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 32px;
  box-shadow: var(--shadow);
  border: 1px solid rgba(255, 255, 255, 0.6);
  position: relative;
}

.article-more-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--text-dim);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  z-index: 10;
  transition: all 0.2s ease;
}

.article-more-btn:hover {
  background: var(--pink-bg);
  color: var(--pink);
}

.article-more-dropdown {
  position: absolute;
  top: 58px;
  right: 18px;
  background: var(--card);
  border-radius: 14px;
  border: 1px solid var(--border-light);
  box-shadow: 0 8px 28px rgba(255, 107, 157, 0.12);
  min-width: 130px;
  padding: 6px;
  z-index: 20;
}

.article-more-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  border-radius: 10px;
  border: none;
  background: transparent;
  color: var(--text);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.18s ease;
  font-family: inherit;
}

.article-more-item:hover {
  background: var(--pink-bg);
  color: var(--pink);
}

.more-item-icon {
  width: 18px;
  height: 18px;
  color: var(--text-dim);
}

.article-more-item:hover .more-item-icon {
  color: var(--warning);
}

.article-header {
  display: flex;
  align-items: center;
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--white);
  box-shadow: var(--shadow-lg);
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.clickable {
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.clickable:hover {
  opacity: 0.8;
}

.article-time {
  font-size: 13px;
  color: var(--text-dim);
}

.article-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--text);
  line-height: 1.4;
  margin-bottom: 28px;
  letter-spacing: 0.02em;
}

.article-content {
  margin-bottom: 24px;
  font-size: 16px;
  line-height: 1.9;
  color: var(--text-secondary);
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  padding: 6px 14px;
  border-radius: 16px;
  background: var(--purple-bg);
  color: var(--purple);
  font-size: 13px;
  font-weight: 500;
}

.article-action-bar {
  display: flex;
  justify-content: space-around;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 10px 20px;
  box-shadow: var(--shadow);
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
  background: var(--pink-bg-hover);
}

.action-btn .action-icon {
  width: 24px;
  height: 24px;
  transition: transform 0.22s ease-out;
}

.action-btn:hover .action-icon {
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

/* ===== 评论区 ===== */
.comment-section {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 14px 20px;
  box-shadow: var(--shadow);
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
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.inline-reply-box {
  margin-left: 68px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--purple-bg);
  border-radius: 12px;
  border: 1px solid var(--border);
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
  background: var(--purple-bg);
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
  background: var(--card);
}

.comment-input::placeholder {
  color: var(--text-dim);
}

.send-btn {
  padding: 10px 24px;
  border-radius: 16px;
  background: var(--gradient-brand);
  color: var(--white);
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  white-space: nowrap;
}

.send-btn:hover:not(:disabled) {
  box-shadow: var(--shadow-lg);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.comment-list {
  display: flex;
  flex-direction: column;
}

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

.back-btn {
  padding: 10px 32px;
  border-radius: 20px;
  background: var(--gradient-brand);
  color: var(--white);
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.back-btn:hover {
  box-shadow: var(--shadow-lg);
}

/* ===== 点击弹跳动效 ===== */
@keyframes articleLikePop {
  0%   { transform: scale(1); }
  35%  { transform: scale(1.4); }
  65%  { transform: scale(0.85); }
  100% { transform: scale(1); }
}

.action-btn.popping .action-icon {
  animation: articleLikePop 0.45s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ===== Dark mode ===== */
html.dark .article-main-card {
  background: rgba(30, 26, 50, 0.75);
  border-color: rgba(255, 182, 215, 0.1);
}

html.dark .article-action-bar {
  background: rgba(30, 26, 50, 0.75);
  border-color: rgba(255, 182, 215, 0.1);
}

html.dark .comment-section {
  background: rgba(30, 26, 50, 0.75);
  border-color: rgba(255, 182, 215, 0.1);
}

html.dark .comment-input {
  background: rgba(30, 26, 50, 0.6);
}

@media (max-width: 768px) {
  .page-inner {
    padding: 0 16px;
  }

  .article-main-card {
    padding: 20px;
  }

  .article-title {
    font-size: 22px;
  }

  .article-action-bar {
    padding: 14px 16px;
  }

  .action-btn {
    padding: 6px 12px;
  }

  .action-btn .action-icon {
    width: 22px;
    height: 22px;
  }

  .comment-section {
    padding: 16px;
  }
}
</style>
