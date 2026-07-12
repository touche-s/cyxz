<template>
  <div class="comment-item" :class="{ 'is-reply': !isTopLevel }">
    <!-- 用户头像 -->
    <img v-if="comment.userAvatar" :src="comment.userAvatar" class="comment-avatar" />
    <div v-else class="comment-avatar-placeholder"></div>

    <div class="comment-body">
      <!-- 头部：用户名 + 时间 -->
      <div class="comment-header">
        <span class="comment-author">{{ comment.userName || '匿名用户' }}</span>
        <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
      </div>

      <!-- 回复提示 -->
      <span v-if="comment.replyToUserName" class="reply-hint">
        回复 @{{ comment.replyToUserName }}
      </span>

      <!-- 内容 -->
      <p class="comment-text">{{ comment.content }}</p>

      <!-- 操作栏 -->
      <div class="comment-actions">
        <button
          class="comment-action-btn"
          :class="{ active: comment.liked }"
          @click="handleToggleLike"
        >
          <img
            :src="comment.liked ? likeIcon : likeOutlineIcon"
            alt="like"
            class="action-icon"
          />
          <span>{{ comment.likes }}</span>
        </button>
        <button class="comment-action-btn" @click="$emit('reply', comment)">
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

      <!-- 子回复区域（仅顶级评论） -->
      <div v-if="isTopLevel && comment.children && comment.children.length > 0" class="replies-section">
        <!-- 已加载的子回复 -->
        <CommentItem
          v-for="child in comment.children"
          :key="child.id"
          :comment="child"
          :is-top-level="false"
          :current-user-id="currentUserId"
          @like="(c) => $emit('like', c)"
          @reply="(c) => $emit('reply', c)"
          @deleted="(id) => $emit('deleted', id)"
        />

        <!-- 展开更多回复 -->
        <button
          v-if="hasMoreReplies && !replyFinished"
          class="load-more-replies"
          :disabled="replyLoading"
          @click="loadMoreReplies"
        >
          {{ replyLoading ? '加载中...' : `展开更多回复 (${totalReplies - loadedReplyCount}条)` }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { toggleCommentLike, deleteComment, getCommentReplies } from '@/api/comment'
import type { CommentVO } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import likeIcon from '@/assets/icons/like.svg'
import likeOutlineIcon from '@/assets/icons/like-outline.svg'

const props = defineProps<{
  comment: CommentVO
  isTopLevel: boolean
  currentUserId: number | null
}>()

const emit = defineEmits<{
  like: [comment: CommentVO]
  reply: [comment: CommentVO]
  deleted: [commentId: number]
}>()

const userStore = useUserStore()

// ===== 点赞 =====
const handleToggleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  const oldLiked = props.comment.liked
  const oldLikes = props.comment.likes

  // 乐观更新
  props.comment.liked = !oldLiked
  props.comment.likes = oldLiked ? Math.max(oldLikes - 1, 0) : oldLikes + 1

  try {
    const res = await toggleCommentLike(props.comment.id)
    if (res.data.code === 200) {
      props.comment.likes = res.data.data
    }
  } catch {
    // 回滚
    props.comment.liked = oldLiked
    props.comment.likes = oldLikes
  }
}

// ===== 删除 =====
const isOwner = computed(() => {
  if (!props.currentUserId) return false
  return props.currentUserId === props.comment.userId
})

const handleDelete = async () => {
  try {
    const res = await deleteComment(props.comment.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      emit('deleted', props.comment.id)
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

// ===== 展开更多子回复 =====
const replyLoading = ref(false)
const replyPage = ref(1) // 后端 /list 已返回第一页
const replyFinished = ref(!props.comment.hasMoreReplies)

const totalReplies = computed(() => props.comment.totalReplies || 0)
const loadedReplyCount = computed(() => props.comment.children?.length || 0)
const hasMoreReplies = computed(() => !replyFinished.value)

const loadMoreReplies = async () => {
  if (replyLoading.value || replyFinished.value) return

  replyLoading.value = true
  const nextPage = replyPage.value + 1

  try {
    const res = await getCommentReplies({
      parentId: props.comment.id,
      page: nextPage,
      size: 5,
    })
    const pageResult = res.data?.data
    if (pageResult?.records?.length > 0) {
      props.comment.children.push(...pageResult.records)
      replyPage.value = nextPage
      if (props.comment.children.length >= totalReplies.value) {
        replyFinished.value = true
      }
    } else {
      replyFinished.value = true
    }
  } catch {
    ElMessage.error('加载回复失败')
  } finally {
    replyLoading.value = false
  }
}

// ===== 时间格式化 =====
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
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 107, 157, 0.06);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-item.is-reply {
  margin-left: 48px;
  padding: 10px 0;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.15);
}

.comment-avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
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

.reply-hint {
  font-size: 12px;
  color: var(--purple);
  margin-bottom: 4px;
  display: inline-block;
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-secondary);
  word-break: break-word;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 4px;
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
  padding: 4px 10px;
  border-radius: 8px;
  transition: all 0.22s ease-out;
}

.comment-action-btn:hover {
  background: rgba(255, 107, 157, 0.08);
  color: var(--pink);
}

.comment-action-btn.active {
  color: var(--pink);
}

.comment-action-btn .action-icon {
  width: 14px;
  height: 14px;
}

.delete-btn:hover {
  color: #e74c3c;
  background: rgba(231, 76, 60, 0.08);
}

/* 子回复区域 */
.replies-section {
  margin-top: 8px;
  padding: 8px 0;
  background: rgba(255, 107, 157, 0.03);
  border-radius: 12px;
}

.load-more-replies {
  display: block;
  width: 100%;
  padding: 8px;
  margin-top: 4px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: var(--purple);
  text-align: left;
  padding-left: 60px;
  transition: all 0.22s ease-out;
}

.load-more-replies:hover:not(:disabled) {
  color: var(--pink);
}

.load-more-replies:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
