<template>
  <div class="comment-item" :class="{ 'is-reply': !isTopLevel }">
    <!-- 用户头像 -->
    <img :src="avatarUrl(comment.userAvatar)"
         class="comment-avatar"
         :class="{ clickable: comment.userId }"
         @click="comment.userId && goToUser()" />

    <div class="comment-body">
      <!-- 头部：用户名 + 内容（子评论时同行显示） -->
      <div class="comment-header">
        <span class="comment-author"
              :class="{ clickable: comment.userId }"
              @click="comment.userId && goToUser()">{{ comment.userName || '匿名用户' }}</span>
        <span v-if="isReply" class="comment-text-inline">
          <template v-if="comment.replyToUserName">
            回复 <span class="reply-mention">@{{ comment.replyToUserName }}</span> : {{ comment.content }}
          </template>
          <template v-else>
            : {{ comment.content }}
          </template>
        </span>
      </div>

      <!-- 内容（仅顶级评论单独一行） -->
      <p v-if="!isReply" class="comment-text">
        <template v-if="comment.replyToUserName">
          回复 <span class="reply-mention">@{{ comment.replyToUserName }}</span> : {{ comment.content }}
        </template>
        <template v-else>
          {{ comment.content }}
        </template>
      </p>

      <!-- 操作栏：时间 + 点赞 + 回复 -->
      <div class="comment-actions">
        <span class="comment-time">{{ formatDateTime(comment.createTime) }}</span>
        <button
          class="comment-action-btn"
          :class="{ active: comment.liked }"
          @click="handleToggleLike"
        >
          <Icon icon="ph:heart" class="action-icon pink-icon" v-show="!comment.liked" />
          <Icon icon="ph:heart-fill" class="action-icon pink-icon" v-show="comment.liked" />
          <span>{{ comment.likes }}</span>
        </button>
        <button class="comment-action-btn" @click="handleReply">
          <span>回复</span>
        </button>
        <button
          v-if="isOwner"
          class="comment-action-btn delete-btn"
          @click="handleDelete"
        >
          <span>删除</span>
        </button>
      </div>

      <!-- 子回复入口（仅顶级评论、有回复时显示） -->
      <div v-if="isTopLevel && totalReplies > 0">
        <!-- 有回复但未加载：点击查看 -->
        <button
          v-if="!replyLoaded"
          class="toggle-replies-btn"
          @click="loadReplies"
        >
          共 {{ totalReplies }} 条回复，点击查看
        </button>

        <!-- 已加载：展示回复列表 + 分页控件 -->
        <template v-else>
          <div class="replies-section">
            <CommentItem
              v-for="child in currentReplyPage"
              :key="child.id"
              :comment="child"
              :is-top-level="false"
              :current-user-id="currentUserId"
              :top-level-id="comment.id"
              :is-reply="true"
              @like="(c) => $emit('like', c)"
              @reply="(payload) => $emit('reply', payload)"
              @deleted="handleChildDeleted"
            />
          </div>

          <!-- 分页控件 -->
          <div class="reply-pagination">
            <span class="pagination-info">共 {{ totalPages }} 页</span>
            <button
              v-if="currentPage > 1"
              class="pagination-btn"
              @click="goToReplyPage(currentPage - 1)"
            >
              上一页
            </button>
            <button
              v-for="page in displayPages"
              :key="page"
              class="pagination-btn"
              :class="{ active: page === currentPage }"
              @click="goToReplyPage(page)"
            >
              {{ page }}
            </button>
            <button
              v-if="currentPage < totalPages"
              class="pagination-btn"
              @click="goToReplyPage(currentPage + 1)"
            >
              下一页
            </button>
            <button class="pagination-btn collapse-btn" @click="hideReplies">
              收起回复
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { likeComment, unlikeComment, deleteComment, getCommentReplies } from '@/api/comment'
import type { CommentVO } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { useNavigate } from '@/composables/useNavigate'
import { Icon } from '@iconify/vue'
import { formatDateTime } from '@/utils/format'
import { avatarUrl } from '@/utils/avatar'

const { open } = useNavigate()

const props = defineProps<{
  comment: CommentVO
  isTopLevel: boolean
  currentUserId: string | null
  topLevelId?: string
  isReply?: boolean
}>()

const emit = defineEmits<{
  like: [comment: CommentVO]
  reply: [payload: { comment: CommentVO; parentId: string }]
  deleted: [commentId: string]
}>()

const handleReply = () => {
  const parentId = props.isTopLevel ? props.comment.id : (props.topLevelId ?? props.comment.id)
  emit('reply', { comment: props.comment, parentId })
}

function goToUser() {
  if (props.comment.userId) {
    open(`/user/${props.comment.userId}`)
  }
}

const userStore = useUserStore()
const { requireLogin } = useAuth()

// ===== 点赞 =====
const likeLoading = ref(false)

const handleToggleLike = async () => {
  if (!requireLogin()) return
  if (likeLoading.value) return

  const oldLiked = props.comment.liked
  const oldLikes = props.comment.likes

  likeLoading.value = true
  props.comment.liked = !oldLiked
  props.comment.likes = oldLiked ? Math.max(oldLikes - 1, 0) : oldLikes + 1

  try {
    if (oldLiked) {
      await unlikeComment(props.comment.id)
    } else {
      await likeComment(props.comment.id)
    }
  } catch {
    props.comment.liked = oldLiked
    props.comment.likes = oldLikes
  } finally {
    likeLoading.value = false
  }
}

// ===== 删除 =====
const isOwner = computed(() => {
  if (!props.currentUserId) return false
  return props.currentUserId === props.comment.userId
})

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '删除评论', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteComment(props.comment.id)
    ElMessage.success('删除成功')
    emit('deleted', props.comment.id)
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('删除失败')
    }
  }
}

/** 子回复被删除时：清理本地缓存并通知父组件更新总数 */
const handleChildDeleted = (childId: string) => {
  // 从已加载的所有回复页中移除该条
  for (let i = 0; i < replyPages.value.length; i++) {
    if (replyPages.value[i]) {
      replyPages.value[i] = replyPages.value[i].filter(c => c.id !== childId)
    }
  }
  // 更新顶部显示的回复数
  if (props.comment.totalReplies != null) {
    props.comment.totalReplies = Math.max(0, props.comment.totalReplies - 1)
  }
  // 继续冒泡到 PostDetail 更新全局 commentTotal
  emit('deleted', childId)
}

// ===== 子回复：分页加载 =====
const replyLoading = ref(false)
const replyLoaded = ref(false)
const currentPage = ref(1)
const replyPageSize = 5

// 存储每页的回复数据
const replyPages = ref<CommentVO[][]>([])

const totalReplies = computed(() => props.comment.totalReplies || 0)
const totalPages = computed(() => Math.ceil(totalReplies.value / replyPageSize))

// 当前页显示的回复
const currentReplyPage = computed(() => {
  return replyPages.value[currentPage.value - 1] || []
})

// 显示的页码按钮（最多显示 5 个）
const displayPages = computed(() => {
  const total = totalPages.value
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1)
  
  const current = currentPage.value
  if (current <= 3) return [1, 2, 3, 4, 5]
  if (current >= total - 2) return [total - 4, total - 3, total - 2, total - 1, total]
  return [current - 2, current - 1, current, current + 1, current + 2]
})

/** 首次加载子回复（第一页） */
const loadReplies = async () => {
  if (replyLoading.value || replyLoaded.value) return
  replyLoading.value = true

  try {
    const res = await getCommentReplies({
      parentId: props.comment.id,
      page: 1,
      size: replyPageSize,
    })
    const pageResult = res
    if (pageResult?.records?.length > 0) {
      replyPages.value[0] = pageResult.records
      replyLoaded.value = true
      currentPage.value = 1
    }
  } catch {
    ElMessage.error('加载回复失败')
  } finally {
    replyLoading.value = false
  }
}

/** 跳转到指定页 */
const goToReplyPage = async (page: number) => {
  if (page < 1 || page > totalPages.value) return
  if (page === currentPage.value) return
  
  // 如果该页已加载，直接切换
  if (replyPages.value[page - 1]) {
    currentPage.value = page
    return
  }
  
  // 否则从后端加载
  replyLoading.value = true
  try {
    const res = await getCommentReplies({
      parentId: props.comment.id,
      page,
      size: replyPageSize,
    })
    const pageResult = res
    if (pageResult?.records) {
      replyPages.value[page - 1] = pageResult.records
      currentPage.value = page
    }
  } catch {
    ElMessage.error('加载回复失败')
  } finally {
    replyLoading.value = false
  }
}

/** 收起回复（保留已加载数据，只隐藏） */
const hideReplies = () => {
  replyLoaded.value = false
  currentPage.value = 1
}

/** 监听回复数变化，直接刷新当前已加载的页（不隐藏 UI，避免闪烁） */
watch(() => props.comment.totalReplies, async (newVal, oldVal) => {
  if (newVal !== oldVal && replyLoaded.value) {
    replyLoading.value = true
    try {
      const res = await getCommentReplies({
        parentId: props.comment.id,
        page: currentPage.value,
        size: replyPageSize,
      })
      const pageResult = res
      if (pageResult?.records) {
        replyPages.value[currentPage.value - 1] = pageResult.records
      }
    } catch {
      // 静默失败
    } finally {
      replyLoading.value = false
    }
  }
})
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-item.is-reply {
  margin-left: 10px;
  padding: 10px 0;
  gap: 8px;
}

/* 顶级评论头像 */
.comment-item:not(.is-reply) .comment-avatar {
  width: 48px;
  height: 48px;
}

.comment-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

/* 子评论头像小一点 */
.comment-item.is-reply .comment-avatar {
  width: 24px;
  height: 24px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-item.is-reply .comment-header {
  margin-bottom: 4px;
}

.comment-author {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.comment-item.is-reply .comment-author {
  font-size: 14px;
}

.clickable {
  cursor: pointer;
  transition: opacity 0.2s ease;
}
.clickable:hover {
  opacity: 0.8;
}

.comment-text-inline {
  font-size: 14px;
  color: var(--text);
  font-weight: 400;
}

.comment-time {
  font-size: 13px;
  color: var(--text-dim);
}

.reply-mention {
  color: var(--purple);
  font-weight: 500;
}

.comment-text {
  font-size: 15px;
  line-height: 1.6;
  color: var(--text);
  word-break: break-word;
  margin-bottom: 8px;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
  line-height: 1;
}

.comment-time {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1;
}

.comment-action-btn {
  display: flex;
  align-items: center;
  gap: 2px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-dim);
  padding: 0 2px;
  border-radius: 4px;
  transition: all 0.2s ease-out;
  line-height: 1;
  height: 24px;
}

.comment-action-btn:hover {
  background: var(--pink-bg);
  color: var(--text);
}

.comment-action-btn.active {
  color: var(--pink);
}

.comment-action-btn .action-icon {
  width: 16px;
  height: 16px;
  vertical-align: middle;
  margin-top: -1px;
}

.delete-btn:hover {
  color: var(--error);
  background: rgba(231, 76, 60, 0.08);
}

/* 子回复区域 */
.replies-section {
  margin-top: 0;
  padding: 0;
}

/* 子回复分页控件 */
.reply-pagination {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-top: 0;
  padding: 0;
}

.pagination-info {
  font-size: 13px;
  line-height: 24px;
  color: var(--text-dim);
  margin-right: 4px;
  white-space: nowrap;
  flex-shrink: 0;
}

.pagination-btn {
  background: transparent;
  border: none;
  outline: none;
  cursor: pointer;
  font-size: 13px;
  line-height: 24px;
  color: var(--purple);
  padding: 0 8px;
  border-radius: 4px;
  transition: all 0.2s ease-out;
  white-space: nowrap;
  flex-shrink: 0;
}

.pagination-btn:hover {
  background: var(--purple-bg);
}

.pagination-btn.active {
  color: var(--purple);
  font-weight: 600;
}

/* 点击查看 / 收起回复 - 简单文字 */
.toggle-replies-btn {
  display: block;
  width: 100%;
  padding: 4px 0;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--text-dim);
  text-align: left;
  transition: color 0.2s ease-out;
}

.toggle-replies-btn:hover {
  color: var(--purple);
}

/* 查看更多回复 - 简单文字 */
.load-more-replies {
  display: block;
  width: 100%;
  padding: 4px 0;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--text-dim);
  text-align: left;
  transition: color 0.2s ease-out;
}

.load-more-replies:hover:not(:disabled) {
  color: var(--purple);
}

.load-more-replies:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
