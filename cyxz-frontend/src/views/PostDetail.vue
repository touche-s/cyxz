<template>
  <div class="post-detail-page">
    <div class="background-image"></div>

    <main class="main-content">
      <div class="page-inner">
        <LoadingSpinner v-if="loading" text="加载中..." />

        <div v-else-if="post" class="post-detail">
          <div class="post-main-card">
            <button class="post-more-btn" @click.stop="showReportMenu = !showReportMenu">
              <Icon icon="ph:dots-three-outline-vertical" />
            </button>
            <div v-if="showReportMenu" class="post-more-dropdown" @click.stop>
              <button class="post-more-item" @click="handleReportFromMenu">
                <Icon icon="ph:warning" class="more-item-icon" />
                举报
              </button>
            </div>
            <div class="post-header">
              <div class="author-info">
                <UserAvatar :src="post.authorAvatar" :name="post.authorName" :user-id="post.userId" :size="56" border="white" shadow="lg" fallback="image" />
                <div class="author-meta">
                  <div class="author-name-row">
                    <span class="author-name clickable" @click="goToAuthor">{{ post.authorName || '匿名用户' }}</span>
                    <FollowButton v-if="post.userId && String(post.userId) !== String(currentUserId)"
                            :following="following"
                            :loading="followLoading"
                            variant="author"
                            @toggle="toggleFollow" />
                  </div>
                  <span class="post-time">{{ formatDateTime(post.createTime) }}</span>
                </div>
              </div>
            </div>

            <h1 class="post-title">{{ post.title }}</h1>

            <div class="post-images" v-if="post.images && post.images.length > 0">
              <div class="carousel-container" :style="{ aspectRatio: carouselAspectRatio }">
                <div class="carousel-track" :style="{ transform: `translateX(-${currentImage * 100}%)` }">
                  <img
                    v-for="(img, index) in post.images"
                    :key="index"
                    :src="img"
                    :alt="`图片${index + 1}`"
                    class="carousel-slide"
                    @load="(e) => onImageLoad(index, e)"
                    @click.stop="openLightbox(index)"
                  />
                </div>

                <!-- 左右箭头 -->
                <button v-if="post.images.length > 1" class="carousel-arrow carousel-prev" @click="prevImage">
                  <Icon icon="ph:caret-left" class="carousel-arrow-icon" />
                </button>
                <button v-if="post.images.length > 1" class="carousel-arrow carousel-next" @click="nextImage">
                  <Icon icon="ph:caret-right" class="carousel-arrow-icon" />
                </button>

                <!-- 底部指示点 -->
                <div class="carousel-dots" v-if="post.images.length > 1">
                  <span v-for="(_, index) in post.images" :key="index" class="carousel-dot" :class="{ active: index === currentImage }" @click="currentImage = index"></span>
                </div>
              </div>
            </div>

            <div class="post-content" v-if="post.postType === 'ARTICLE'">
              <v-md-preview :text="post.content" />
            </div>
            <div class="post-content" v-else>
              <p v-for="(paragraph, index) in contentParagraphs" :key="index" class="content-paragraph">
                {{ paragraph }}
              </p>
            </div>

            <div class="post-tags" v-if="post.tags && post.tags.length > 0">
              <span v-for="tag in post.tags" :key="tag" class="tag-item">#{{ tag }}</span>
            </div>
          </div>

          <div class="post-action-bar">
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

            <!-- 顶部评论框：始终显示，用于回复帖子 -->
            <div class="comment-input-area">
              <div class="input-row">
                <textarea
                  v-model="commentInput"
                  class="comment-input"
                  placeholder="写下你的评论..."
                  rows="3"
                  maxlength="500"
                ></textarea>
                <button class="send-btn" :disabled="!commentInput.trim()" @click="submitTopComment">
                  发送
                </button>
              </div>
              <span class="comment-char-count">{{ commentInput.length }}/500</span>
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
                      maxlength="500"
                      ref="inlineReplyInput"
                    ></textarea>
                    <button class="send-btn" :disabled="!commentInput.trim()" @click="submitComment">
                      发送
                    </button>
                  </div>
                  <span class="comment-char-count">{{ commentInput.length }}/500</span>
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

            <EmptyState v-else-if="!commentLoading" icon="ph:chat-circle-text" title="暂无评论，来发表第一条评论吧~" />

            <div class="empty-comments" v-else>
              <p>加载中...</p>
            </div>
          </div>
        </div>

        <EmptyState v-else :icon="errorState.icon" :title="errorState.title" :hint="errorState.hint">
          <template #actions>
            <button class="back-btn" @click="goHome">返回首页</button>
          </template>
        </EmptyState>
      </div>
    </main>

    <!-- 图片放大预览 -->
    <Teleport to="body">
      <Transition name="lightbox-fade">
        <div v-if="lightboxVisible" class="lightbox-overlay" @click.self="closeLightbox" @keydown="handleLightboxKeydown" tabindex="0" ref="lightboxOverlay">
          <div class="lightbox-image-wrap">
            <img :src="lightboxImages[lightboxIndex]" class="lightbox-image" alt="预览图片" />
          </div>

          <button class="lightbox-close" @click="closeLightbox">
            <Icon icon="ph:x" class="lightbox-close-icon" />
          </button>

          <template v-if="lightboxImages.length > 1">
            <button class="lightbox-arrow lightbox-prev" @click.stop="lightboxPrev">
              <Icon icon="ph:caret-left" class="lightbox-arrow-icon" />
            </button>
            <button class="lightbox-arrow lightbox-next" @click.stop="lightboxNext">
              <Icon icon="ph:caret-right" class="lightbox-arrow-icon" />
            </button>
          </template>

          <div class="lightbox-counter" v-if="lightboxImages.length > 1">{{ lightboxIndex + 1 }} / {{ lightboxImages.length }}</div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useNavigate } from '@/composables/useNavigate'
import { ElMessage } from 'element-plus'
import { getPostDetail, likePost, unlikePost, collectPost, uncollectPost, recordPostView } from '@/api/post'
import { isPublished } from '@/utils/postStatus'
import { formatDateTime, formatNumber } from '@/utils/format'
import { ErrorCode } from '@/utils/errorCode'
import { ApiError } from '@/utils/request'
import UserAvatar from '@/components/UserAvatar.vue'
import {
  getCommentList,
  createComment,
} from '@/api/comment'
import type { PostVO } from '@/api/post'
import type { CommentVO, CreateCommentRequest } from '@/api/comment'
import type { PageResult } from '@/api/types/common'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { useFollow } from '@/composables/useFollow'
import CommentItem from '@/components/CommentItem.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import FollowButton from '@/components/FollowButton.vue'
import { useToggleInteraction } from '@/composables/useToggleInteraction'
import { usePagination } from '@/composables/usePagination'
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
// 帖子加载失败时的细分错误状态
const errorState = ref<{ icon: string; title: string; hint?: string }>({
  icon: 'ph:warning-circle',
  title: '帖子不存在或已删除',
})
const commentInput = ref('')
const commentSection = ref<HTMLElement | null>(null)
const currentImage = ref(0)


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
// ===== 图片轮播动态比例 =====
const imageNaturalRatios = ref<number[]>([])
function onImageLoad(index: number, e: Event) {
  const img = e.target as HTMLImageElement
  imageNaturalRatios.value[index] = img.naturalWidth / img.naturalHeight
}
const carouselAspectRatio = computed(() => {
  const ratio = imageNaturalRatios.value[currentImage.value]
  return ratio ? `${ratio}` : '4/3'
})

const replyTarget = ref<{ comment: CommentVO; parentId: string } | null>(null)
const activeReplyId = ref<string | null>(null) // 哪个顶级评论下方显示回复框


// ===== 评论列表 =====
const {
  list: comments,
  loading: commentLoading,
  hasMore: commentHasMore,
  load: loadCommentsPagination,
  loadMore: loadMoreComments,
  unshift: unshiftComment,
  removeById: removeCommentById,
} = usePagination<CommentVO>(
  ({ page, size }) => getCommentList({
    postId: String(post.value?.id ?? ''),
    page,
    size,
  }),
  {
    pageSize: 20,
    onError: () => ElMessage.error('加载评论失败'),
    onPageLoaded: (result) => {
      const pr = result as PageResult<CommentVO>
      if (post.value) post.value.comments = pr.total ?? 0
    },
  }
)
const commentFinished = computed(() => !commentHasMore.value)

async function loadComments(reset = false) {
  await loadCommentsPagination(reset)
}

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

function goToAuthor() {
  if (post.value?.userId) {
    open(`/user/${post.value.userId}`)
  }
}

const prevImage = () => {
  if (currentImage.value > 0) currentImage.value--
}

const nextImage = () => {
  if (post.value?.images && currentImage.value < post.value.images.length - 1) {
    currentImage.value++
  }
}

// ===== 图片放大预览 =====
const lightboxVisible = ref(false)
const lightboxIndex = ref(0)
const lightboxImages = computed(() => post.value?.images || [])
const lightboxOverlay = ref<HTMLElement | null>(null)

function openLightbox(index: number) {
  lightboxIndex.value = index
  lightboxVisible.value = true
  nextTick(() => {
    lightboxOverlay.value?.focus()
  })
}

function closeLightbox() {
  lightboxVisible.value = false
}

function lightboxPrev() {
  if (lightboxIndex.value > 0) lightboxIndex.value--
}

function lightboxNext() {
  if (lightboxIndex.value < lightboxImages.value.length - 1) lightboxIndex.value++
}

function handleLightboxKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    closeLightbox()
  } else if (e.key === 'ArrowLeft') {
    lightboxPrev()
  } else if (e.key === 'ArrowRight') {
    lightboxNext()
  }
}

// ===== 帖子 =====
const loadPost = async () => {
  const postId = String(route.params.id)
  loading.value = true
  try {
    const data = await getPostDetail(postId)
    post.value = data as PostVO
    // 非已发布内容不进详情页
    if (!isPublished(post.value.status)) {
      post.value = null
      ElMessage.warning('该内容不可查看')
      loading.value = false
      return
    }
    // 非作者本人时查询关注状态
    if (post.value.userId && String(post.value.userId) !== String(currentUserId.value)) {
      await checkFollowing(String(post.value.userId))
    }
  } catch (e) {
    post.value = null
    // 按细分错误码设置差异化提示
    if (e instanceof ApiError) {
      switch (e.code) {
        case ErrorCode.POST_DELETED:
          errorState.value = { icon: 'ph:trash', title: '帖子已被作者删除', hint: '看看其他精彩内容吧~' }
          break
        case ErrorCode.POST_REJECTED:
          errorState.value = { icon: 'ph:shield-warning', title: '该帖子因违规被下架', hint: '如有疑问请联系管理员' }
          break
        case ErrorCode.POST_PENDING:
          errorState.value = { icon: 'ph:hourglass', title: '帖子正在审核中', hint: '稍后再来看看吧~' }
          break
        case ErrorCode.POST_NOT_INTERACTABLE:
          errorState.value = { icon: 'ph:lock', title: '该内容暂不可查看', hint: '作者尚未发布此内容' }
          break
        case ErrorCode.POST_NOT_FOUND:
          errorState.value = { icon: 'ph:warning-circle', title: '帖子不存在', hint: '可能已被删除或链接有误' }
          break
        default:
          errorState.value = { icon: 'ph:warning-circle', title: '加载失败', hint: '请稍后重试' }
      }
    } else {
      errorState.value = { icon: 'ph:warning-circle', title: '加载失败', hint: '请检查网络后重试' }
    }
  } finally {
    loading.value = false
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

const handleReport = () => {
  ElMessage.info('举报功能即将上线。如遇违规内容，请联系管理员。')
}

const showReportMenu = ref(false)

function handleReportFromMenu() {
  showReportMenu.value = false
  ElMessage.info('举报功能即将上线。如遇违规内容，请联系管理员。')
}

function onDocClick() {
  showReportMenu.value = false
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

// ===== 发表评论 / 回复 =====
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
    // 后端返回完整 CommentVO，直接插入列表第一条展示
    unshiftComment(newComment)
    if (post.value) post.value.comments = (post.value.comments ?? 0) + 1
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '评论失败'
    if (e instanceof ApiError && e.code === ErrorCode.CONTENT_SENSITIVE) {
      ElMessage.error({ message: msg, duration: 5000 })
    } else {
      ElMessage.error(msg)
    }
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
    // 局部更新：回复数 +1，同时把新回复插入当前展开的 children 列表
    const idx = comments.value.findIndex(c => c.id === replyParentId)
    if (idx !== -1) {
      const updated = {
        ...comments.value[idx],
        totalReplies: (comments.value[idx].totalReplies || 0) + 1,
      }
      // 如果当前父评论的 children 已展开，把新回复追加到末尾
      if (updated.children && updated.children.length > 0) {
        updated.children = [...updated.children, newComment]
      }
      comments.value[idx] = updated
    }
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '回复失败'
    if (e instanceof ApiError && e.code === ErrorCode.CONTENT_SENSITIVE) {
      ElMessage.error({ message: msg, duration: 5000 })
    } else {
      ElMessage.error(msg)
    }
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
  // 从顶级列表移除（如果是顶级评论）；子回复由 CommentItem 本地清理
  removeCommentById('id', commentId)
  if (post.value) post.value.comments = Math.max(0, (post.value.comments ?? 1) - 1)
}

const goHome = () => {
  to('/')
}
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
  box-shadow: var(--shadow);
  border: 1px solid rgba(255, 255, 255, 0.6);
  position: relative;
}

.post-more-btn {
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

.post-more-btn:hover {
  background: var(--pink-bg);
  color: var(--pink);
}

.post-more-dropdown {
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

.post-more-item {
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

.post-more-item:hover {
  background: var(--pink-bg);
  color: var(--pink);
}

.more-item-icon {
  width: 18px;
  height: 18px;
  color: var(--text-dim);
}

.post-more-item:hover .more-item-icon {
  color: var(--warning);
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
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
  max-height: 60vh;
  overflow: hidden;
  background: var(--card);
  transition: height 0.35s ease, aspect-ratio 0.35s ease;
}

.carousel-track {
  display: flex;
  height: 100%;
  transition: transform 0.3s ease;
}

.carousel-slide {
  min-width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center;
  cursor: pointer;
  user-select: none;
}

.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  color: var(--white);
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
  background: var(--card);
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
  background: var(--purple-bg);
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

.action-btn img,
.action-btn .action-icon {
  width: 24px;
  height: 24px;
  transition: transform 0.22s ease-out;
}

.action-btn:hover img,
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


/* ===== 评论顶部输入框 ===== */
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

/* 评论输入区 */
.comment-input-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.comment-char-count {
  align-self: flex-end;
  font-size: 12px;
  color: var(--text-dim);
  padding-right: 4px;
}

/* 内联回复框 */
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

/* ===== 图片放大预览 Lightbox ===== */
.lightbox-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(18, 12, 27, 0.62);
  backdrop-filter: blur(20px) saturate(140%);
  display: flex;
  align-items: center;
  justify-content: center;
  outline: none;
}

.lightbox-image-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 90vw;
  max-height: 85vh;
}

.lightbox-image {
  max-width: 90vw;
  max-height: 85vh;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.lightbox-close {
  position: absolute;
  top: 24px;
  right: 28px;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  color: var(--white);
  border: 1px solid rgba(255, 255, 255, 0.25);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.24s ease;
  z-index: 10;
}

.lightbox-close:hover {
  background: rgba(255, 107, 157, 0.6);
  border-color: rgba(255, 107, 157, 0.5);
  transform: rotate(90deg) scale(1.05);
}

.lightbox-close-icon {
  width: 18px;
  height: 18px;
}

.lightbox-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  color: var(--white);
  border: 1px solid rgba(255, 255, 255, 0.22);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease;
  z-index: 10;
}

.lightbox-arrow:hover {
  background: rgba(255, 107, 157, 0.45);
  border-color: rgba(255, 107, 157, 0.4);
  transform: translateY(-50%) scale(1.08);
}

.lightbox-arrow-icon {
  width: 28px;
  height: 28px;
}

.lightbox-prev { left: 28px; }
.lightbox-next { right: 28px; }

.lightbox-counter {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  padding: 6px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  color: var(--white);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.05em;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.lightbox-fade-enter-active,
.lightbox-fade-leave-active {
  transition: opacity 0.28s ease;
}

.lightbox-fade-enter-from,
.lightbox-fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .lightbox-close {
    top: 16px;
    right: 16px;
    width: 36px;
    height: 36px;
  }

  .lightbox-arrow {
    width: 40px;
    height: 40px;
  }

  .lightbox-prev { left: 12px; }
  .lightbox-next { right: 12px; }

  .lightbox-image {
    max-width: 95vw;
    max-height: 80vh;
  }
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

/* ===== 点击弹跳动效 ===== */
@keyframes postLikePop {
  0%   { transform: scale(1); }
  35%  { transform: scale(1.4); }
  65%  { transform: scale(0.85); }
  100% { transform: scale(1); }
}

.action-btn.popping img {
  animation: postLikePop 0.45s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ===== Dark mode overrides ===== */
html.dark .post-main-card {
  background: rgba(30, 26, 50, 0.75);
  border-color: rgba(255, 182, 215, 0.1);
}

html.dark .post-action-bar {
  background: rgba(30, 26, 50, 0.75);
  border-color: rgba(255, 182, 215, 0.1);
}

html.dark .comment-section {
  background: rgba(30, 26, 50, 0.75);
  border-color: rgba(255, 182, 215, 0.1);
}

html.dark .carousel-dot {
  background: rgba(30, 26, 50, 0.5);
}

html.dark .comment-input {
  background: rgba(30, 26, 50, 0.6);
}

html.dark .lightbox-close {
  background: rgba(255, 107, 157, 0.04);
  border-color: rgba(255, 107, 157, 0.06);
}

html.dark .lightbox-arrow {
  background: rgba(255, 107, 157, 0.04);
  border-color: rgba(255, 107, 157, 0.06);
}

html.dark .lightbox-counter {
  background: rgba(255, 107, 157, 0.04);
  border-color: rgba(255, 107, 157, 0.06);
}
</style>
