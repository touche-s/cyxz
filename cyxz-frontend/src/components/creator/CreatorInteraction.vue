<template>
  <div class="page-container">
    <header class="page-header">
      <div class="header-left">
        <h1>评论管理</h1>
        <p>管理你作品下的评论内容</p>
      </div>
    </header>

    <div class="filter-bar">
      <div class="filter-group comment-filter-group">
        <el-select
          v-model="selectedCommentPostId"
          placeholder="全部帖子"
          class="comment-post-select"
          popper-class="comment-post-select-popper"
          clearable
          @change="handleCommentPostFilterChange"
        >
          <el-option value="" label="全部帖子" />
          <el-option
            v-for="post in publishedPostOptions"
            :key="post.id"
            :value="post.id"
            :label="post.title"
          >
            <div class="comment-post-option">
              <span class="comment-post-option-title">{{ post.title }}</span>
              <span class="comment-post-option-status" :class="`status-${post.status}`">{{ statusText(post.status) }}</span>
            </div>
          </el-option>
        </el-select>
        <span class="comment-total">共 {{ commentsTotal }} 条评论</span>
      </div>
      <div class="sort-group">
        <button
          class="sort-btn"
          :class="{ active: !commentSortAsc }"
          @click="handleCommentSortChange(false)"
        >最新</button>
        <button
          class="sort-btn"
          :class="{ active: commentSortAsc }"
          @click="handleCommentSortChange(true)"
        >最早</button>
      </div>
    </div>

    <div class="search-bar">
      <SearchInput v-model="commentSearchKeyword" variant="inline" placeholder="搜索评论内容或用户名..." />
    </div>

    <div class="comment-list" v-if="!commentsLoading && filteredManagedComments.length > 0">
      <div class="comment-manage-item" v-for="comment in filteredManagedComments" :key="comment.id">
        <div class="comment-avatar clickable" @click="goToUser(comment.userId)">
          <img :src="avatarUrl(comment.userAvatar)" alt="" />
        </div>
        <div class="comment-body">
          <div class="comment-top-row">
            <span class="comment-name clickable" @click="goToUser(comment.userId)">{{ comment.userName }}</span>
            <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
          </div>
          <div class="comment-main-row">
            <span class="comment-context">
              来自作品：<span class="context-post-title" @click="viewPost(comment.postId)" :title="comment.postTitle">{{ truncatePostTitle(comment.postTitle, comment.postId) }}</span>
              <template v-if="comment.replyToUserName">
                ，回复了 <span class="context-reply-to">@{{ comment.replyToUserName }}</span>
              </template>
            </span>
            <span class="comment-content">{{ comment.content }}</span>
          </div>
        </div>
        <button class="comment-delete-btn" @click="confirmDeleteComment(comment)" title="删除评论">
          <Icon icon="ph:trash" class="delete-icon" />
        </button>
      </div>
    </div>

    <LoadingSpinner v-else-if="commentsLoading" text="加载中..." />

    <EmptyState v-else :icon="iconEmpty" :title="commentSearchKeyword ? '没有匹配的评论' : (selectedCommentPostId ? '当前帖子还没有评论' : '当前还没有人给你的作品留言')" />

    <Pagination :current="commentPage" :total="commentsTotal" :page-size="commentPageSize" @change="handleCommentPageChange" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import SearchInput from '@/components/SearchInput.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import Pagination from '@/components/Pagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getManagedComments } from '@/api/comment'
import { getUserPosts } from '@/api/post'
import type { CommentVO } from '@/api/comment'
import type { PostVO } from '@/api/post'
import { formatTime } from '@/utils/format'
import { avatarUrl } from '@/utils/avatar'
import { useNavigate } from '@/composables/useNavigate'
import { isDeleted, statusText } from '@/utils/postStatus'

const emit = defineEmits<{
  deleteComment: [comment: CommentVO]
}>()

const { open } = useNavigate()

const posts = ref<PostVO[]>([])
const managedCommentsList = ref<CommentVO[]>([])
const commentsTotal = ref(0)
const commentsLoading = ref(false)
const selectedCommentPostId = ref('')
const commentSortAsc = ref(false)
const commentSearchKeyword = ref('')
const commentPage = ref(1)
const commentPageSize = 20

const iconEmpty = 'ph:tray'

const publishedPostOptions = computed(() => {
  return posts.value.filter(p => !isDeleted(p.status))
})

const filteredManagedComments = computed(() => {
  const kw = commentSearchKeyword.value.trim().toLowerCase()
  if (!kw) return managedCommentsList.value
  return managedCommentsList.value.filter(c =>
    c.content?.toLowerCase().includes(kw) ||
    (c.userName || '').toLowerCase().includes(kw)
  )
})

const truncatePostTitle = (title: string | undefined, postId: string) => {
  const name = title || '帖子' + postId
  return name.length > 12 ? name.slice(0, 12) + '...' : name
}

const loadPosts = async () => {
  try {
    const data = await getUserPosts({ page: 1, size: 100 })
    posts.value = data.records || []
  } catch (error) {
    console.error('加载帖子失败:', error)
  }
}

const loadManagedComments = async () => {
  commentsLoading.value = true
  try {
    const params: { page: number; size: number; postId?: string; sortAsc?: boolean } = {
      page: commentPage.value,
      size: commentPageSize,
      sortAsc: commentSortAsc.value,
    }
    if (selectedCommentPostId.value) {
      params.postId = selectedCommentPostId.value
    }
    const data = await getManagedComments(params)
    managedCommentsList.value = data.records || []
    commentsTotal.value = data.total || 0
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    commentsLoading.value = false
  }
}

const handleCommentPostFilterChange = () => {
  commentPage.value = 1
  commentSearchKeyword.value = ''
  loadManagedComments()
}

const handleCommentSortChange = (sortAsc: boolean) => {
  commentSortAsc.value = sortAsc
  commentPage.value = 1
  loadManagedComments()
}

const handleCommentPageChange = (page: number) => {
  commentPage.value = page
  loadManagedComments()
}

const confirmDeleteComment = (comment: CommentVO) => {
  emit('deleteComment', comment)
}

const goToUser = (userId: string | number) => {
  open(`/user/${userId}`)
}

const viewPost = (postId: string) => {
  open(`/post/${postId}`)
}

onMounted(() => {
  loadPosts()
  loadManagedComments()
})
</script>

<style scoped>
.page-container {
  background: var(--card);
  border-radius: 20px;
  padding: 28px;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.06);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.header-left h1 {
  font-size: 26px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 4px;
}

.page-container .page-header h1 {
  font-size: 24px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-left p {
  font-size: 14px;
  color: var(--text-dim);
}

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.filter-group {
  display: flex;
  gap: 6px;
  background: var(--pink-bg);
  border-radius: 12px;
  padding: 4px;
}

.comment-filter-group {
  display: flex;
  align-items: center;
  background: transparent;
  border-radius: 0;
  padding: 0;
}

.comment-post-select {
  min-width: 180px;
}

/* el-select 粉色圆角定制 */
.comment-post-select {
  --el-color-primary: #FF6B9D;
  --el-color-primary-light-3: #ff8fb5;
  --el-color-primary-light-5: #ffb6cc;
  --el-border-color-hover: #FF6B9D;
  --el-border-color: #FFB6CC;
  --el-input-focus-border-color: #FF6B9D;
}

.comment-post-select :deep(.el-input__wrapper) {
  border-radius: 20px;
  border-color: #FFB6CC;
  background: var(--bg);
  box-shadow: none;
  transition: all 0.22s ease-out;
}

.comment-post-select :deep(.el-input__wrapper:hover) {
  border-color: #FF6B9D;
  background: var(--bg);
}

.comment-post-select :deep(.el-input__wrapper.is-focus),
.comment-post-select :deep(.el-select__wrapper.is-focused),
.comment-post-select :deep(.el-input.is-focus .el-input__wrapper) {
  border-color: #FF6B9D !important;
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.15) !important;
  background: var(--card);
}

.comment-post-select :deep(.el-select__caret) {
  color: #FF6B9D;
}

.comment-post-select :deep(.el-select__placeholder) {
  color: var(--text-dim);
}

.comment-total {
  font-size: 13px;
  color: var(--text-dim);
  margin-left: 4px;
  white-space: nowrap;
}

/* 帖子下拉选项 */
.comment-post-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.comment-post-option-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-post-option-status {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.comment-post-option-status.status-0 {
  color: #8b5cf6;
  background: rgba(139, 92, 246, 0.12);
}

.comment-post-option-status.status-1 {
  color: #16a34a;
  background: rgba(22, 163, 74, 0.12);
}

.comment-post-option-status.status-2 {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.12);
}

/* ===== 排序按钮组 ===== */
.sort-group {
  display: flex;
  gap: 2px;
  background: var(--pink-bg);
  border-radius: 8px;
  padding: 3px;
}

.sort-btn {
  padding: 5px 14px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-dim);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.sort-btn:hover:not(.active) {
  color: var(--pink);
}

.sort-btn.active {
  background: var(--card);
  color: var(--pink);
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

/* ===== 搜索栏 ===== */
.search-bar {
  margin-top: 16px;
  margin-bottom: 20px;
  max-width: 360px;
}

/* ===== 评论列表（扁平布局） ===== */
.comment-list {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--border);
}

.comment-manage-item {
  display: flex;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
  transition: background 0.15s;
}

.comment-manage-item:hover {
  background: rgba(0, 0, 0, 0.012);
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-avatar.clickable,
.comment-name.clickable {
  cursor: pointer;
  transition: opacity 0.15s;
}

.comment-avatar.clickable:hover,
.comment-name.clickable:hover {
  opacity: 0.75;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-top-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 6px;
}

.comment-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.comment-time {
  font-size: 12px;
  color: var(--text-dim);
}

.comment-main-row {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 0;
  line-height: 1.65;
}

.comment-context {
  font-size: 13px;
  color: var(--text-dim);
  white-space: nowrap;
  margin-right: 6px;
}

.context-post-title {
  color: var(--pink);
  cursor: pointer;
  transition: opacity 0.15s;
}

.context-post-title:hover {
  opacity: 0.75;
}

.context-reply-to {
  color: var(--purple);
}

.comment-content {
  font-size: 14px;
  color: var(--text);
  word-break: break-word;
}

.comment-delete-btn {
  padding: 4px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
  align-self: flex-start;
  transition: background 0.15s;
  line-height: 0;
}

.comment-delete-btn:hover {
  background: rgba(239, 68, 68, 0.08);
}

.delete-icon {
  width: 15px;
  height: 15px;
  opacity: 0.35;
  color: var(--text-dim);
  transition: opacity 0.15s, color 0.15s;
}

.comment-delete-btn:hover .delete-icon {
  opacity: 0.7;
  color: #f44336;
}

.comment-manage-item .delete-icon {
  opacity: 0.35;
  transition: opacity 0.15s;
}

.comment-manage-item:hover .delete-icon {
  opacity: 0.6;
  color: #f44336;
}
</style>
