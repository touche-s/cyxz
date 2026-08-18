<template>
  <div class="page-container">
    <header class="page-header">
      <div class="header-left">
        <h1>内容管理</h1>
        <p>管理你的所有作品</p>
      </div>
      <button class="publish-btn" @click="$emit('createPost')">
        <Icon icon="ph:pencil-simple" class="btn-icon" />
        <span>去创作</span>
      </button>
    </header>

    <div class="filter-bar">
      <div class="filter-left">
        <div class="filter-group">
          <button
            v-for="tab in contentTabs"
            :key="tab.value"
            class="filter-btn"
            :class="{ active: activeContentTab === tab.value }"
            @click="activeContentTab = tab.value"
          >
            {{ tab.label }}
            <span class="filter-count">{{ tab.count }}</span>
          </button>
        </div>
        <button class="refresh-btn" :class="{ spinning: loading }" @click="refreshPosts" title="刷新列表">
          <Icon icon="ph:arrow-counter-clockwise" class="refresh-icon" />
        </button>
        <button class="batch-toggle-btn" :class="{ active: batchMode }" @click="toggleBatchMode">
          <Icon icon="ph:check-square" class="batch-toggle-icon" />
          <span>批量操作</span>
        </button>
      </div>
      <div class="sort-group">
        <button
          v-for="opt in sortOptions"
          :key="opt.value"
          class="sort-btn"
          :class="{ active: contentSortField === opt.value }"
          @click="handleContentSort(opt.value)"
        >
          {{ opt.label }}
          <span v-if="contentSortField === opt.value" class="sort-arrow">{{ contentSortOrder === 'asc' ? '↑' : '↓' }}</span>
        </button>
      </div>
    </div>

    <div class="search-bar">
      <SearchInput v-model="searchKeyword" variant="inline" placeholder="搜索当前板块下的作品标题..." />
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-bar" v-if="batchMode && selectedPostIds.length > 0">
      <span class="batch-count">已选 {{ selectedPostIds.length }} 项</span>
      <div class="batch-actions">
        <button class="batch-btn batch-publish" @click="handleBatchPublish">批量发布</button>
        <button class="batch-btn batch-delete" @click="handleBatchDelete">批量删除</button>
      </div>
      <button class="batch-clear" @click="selectedPostIds = []">清除选择</button>
    </div>

    <!-- 全选 -->
    <div class="select-all-bar" v-if="batchMode && filteredContentPosts.length > 0 && !loading">
      <label class="checkbox-label">
        <input type="checkbox" :checked="isAllSelected" @change="toggleAll" />
        <span>全选</span>
      </label>
    </div>

    <div class="content-list" v-if="!loading && filteredContentPosts.length > 0">
      <div class="content-item" v-for="post in filteredContentPosts" :key="post.id">
        <label class="content-checkbox" v-if="batchMode">
          <input type="checkbox" :checked="selectedPostIds.includes(post.id)" @change="togglePost(post.id)" />
        </label>
        <div class="content-cover" :class="{ clickable: isApproved(post.status) }" @click="isApproved(post.status) && $emit('view', post.id)">
          <img v-if="post.cover" :src="post.cover" alt="cover" />
          <div v-else class="cover-placeholder">
            <span>暂无封面</span>
          </div>
        </div>
        <div class="content-info">
          <h3 class="content-title" :class="{ clickable: isApproved(post.status) }" @click="isApproved(post.status) && $emit('view', post.id)">
            <span class="title-text">{{ post.title }}</span>
            <span class="pin-tag" v-if="post.pinned"><Icon icon="ph:push-pin-fill" />置顶</span>
          </h3>
          <div class="content-meta">
            <span class="section-tag" v-if="post.sectionName">{{ post.sectionName }}</span>
            <span class="content-time">{{ formatDateTime(post.createTime) }}</span>
          </div>
          <div class="content-stats">
            <span class="stat-item"><Icon icon="ph:eye" class="stat-mini-icon pink-icon" />{{ post.views }}</span>
            <span class="stat-item"><Icon icon="ph:heart" class="stat-mini-icon pink-icon" />{{ post.likes }}</span>
            <span class="stat-item"><Icon icon="ph:star" class="stat-mini-icon pink-icon" />{{ post.collections }}</span>
          </div>
        </div>
        <div class="content-status">
          <span class="status-tag" :class="'status-' + post.status">
            {{ statusText(post.status) }}
          </span>
          <span
            v-if="post.reviewReason"
            class="review-reason"
            :title="post.reviewReason"
          >{{ post.reviewReason }}</span>
        </div>
        <div class="content-actions">
          <button v-if="isApproved(post.status) && !post.pinned" class="action-btn pin" @click="handlePin(post.id)" title="置顶">
            <Icon icon="ph:push-pin" class="action-icon pin-icon" />
          </button>
          <button v-if="post.pinned" class="action-btn unpin" @click="handleUnpin(post.id)" title="取消置顶">
            <Icon icon="ph:push-pin-simple" class="action-icon unpin-icon" />
          </button>
          <button v-if="!isDeleted(post.status) && !isPending(post.status)" class="action-btn edit" @click="$emit('edit', post.id)" title="编辑">
            <Icon icon="ph:pencil-simple" class="action-icon pink-icon" />
          </button>
          <button v-if="isDraft(post.status)" class="action-btn preview" @click="$emit('preview', post)" title="预览">
            <Icon icon="ph:stack" class="action-icon-svg" />
          </button>
          <button
            v-if="isDraft(post.status)"
            class="action-btn publish"
            @click="$emit('publish', post.id)"
            title="发布"
          >
            <Icon icon="ph:rocket-launch" class="action-icon" />
          </button>
          <button
            v-if="isDeleted(post.status)"
            class="action-btn restore"
            @click="$emit('restore', post.id)"
            title="恢复"
          >
            <Icon icon="ph:arrow-counter-clockwise" class="action-icon" />
          </button>
          <button class="action-btn delete" @click="$emit('delete', post)" :title="isDeleted(post.status) ? '彻底删除' : '删除'">
            <Icon icon="ph:trash" class="action-icon" />
          </button>
        </div>
      </div>
    </div>

    <EmptyState v-else-if="!loading" :icon="iconEmpty" :title="`还没有${activeContentTab === 'published' ? '已通过的' : activeContentTab === 'pending' ? '待审核的' : activeContentTab === 'rejected' ? '被拒绝的' : activeContentTab === 'draft' ? '草稿' : activeContentTab === 'deleted' ? '已删除的' : ''}作品`">
      <template #actions>
        <button class="create-btn" @click="$emit('createPost')">去创作</button>
      </template>
    </EmptyState>

    <LoadingSpinner v-else text="加载中..." />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import SearchInput from '@/components/SearchInput.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getUserPosts, pinPost, unpinPost, batchOperate } from '@/api/post'
import { formatDateTime } from '@/utils/format'
import type { PostVO } from '@/api/post'
import { isDraft, isPending, isApproved, isRejected, isDeleted, statusText } from '@/utils/postStatus'
import { useUserStore } from '@/stores/user'
import { useApi } from '@/composables/useApi'
import { ElMessage, ElMessageBox } from 'element-plus'

defineEmits<{
  'edit': [postId: string]
  'view': [postId: string]
  'createPost': []
  'publish': [postId: string]
  'restore': [postId: string]
  'delete': [post: PostVO]
  'preview': [post: PostVO]
}>()

const userStore = useUserStore()

const posts = ref<PostVO[]>([])
const { loading, run } = useApi()
const activeContentTab = ref<'all' | 'published' | 'pending' | 'rejected' | 'draft' | 'deleted'>('all')
const searchKeyword = ref('')
const contentSortField = ref('create_time')
const contentSortOrder = ref('desc')
const selectedPostIds = ref<string[]>([])
const batchMode = ref(false)

const toggleBatchMode = () => {
  batchMode.value = !batchMode.value
  if (!batchMode.value) {
    selectedPostIds.value = []
  }
}

const iconEmpty = 'ph:tray'

const contentTabs = computed(() => {
  const activePosts = posts.value.filter(p => !isDeleted(p.status) && !isRejected(p.status))
  return [
    { label: '全部', value: 'all' as const, count: activePosts.length },
    { label: '已通过', value: 'published' as const, count: posts.value.filter(p => isApproved(p.status)).length },
    { label: '待审核', value: 'pending' as const, count: posts.value.filter(p => isPending(p.status)).length },
    { label: '拒绝', value: 'rejected' as const, count: posts.value.filter(p => isRejected(p.status)).length },
    { label: '草稿', value: 'draft' as const, count: posts.value.filter(p => isDraft(p.status)).length },
    { label: '已删除', value: 'deleted' as const, count: posts.value.filter(p => isDeleted(p.status)).length },
  ]
})

const filteredContentPosts = computed(() => {
  let filtered = posts.value

  switch (activeContentTab.value) {
    case 'published':
      filtered = filtered.filter(p => isApproved(p.status))
      break
    case 'pending':
      filtered = filtered.filter(p => isPending(p.status))
      break
    case 'rejected':
      filtered = filtered.filter(p => isRejected(p.status))
      break
    case 'draft':
      filtered = filtered.filter(p => isDraft(p.status))
      break
    case 'deleted':
      filtered = filtered.filter(p => isDeleted(p.status))
      break
    default:
      filtered = filtered.filter(p => !isDeleted(p.status) && !isRejected(p.status))
  }

  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.trim().toLowerCase()
    filtered = filtered.filter(p => p.title.toLowerCase().includes(keyword))
  }

  return filtered
})

const sortOptions = [
  { label: '创建时间', value: 'create_time' },
  { label: '浏览量', value: 'views' },
  { label: '点赞数', value: 'likes' },
  { label: '收藏数', value: 'collections' },
]

const handleContentSort = (field: string) => {
  if (contentSortField.value === field) {
    contentSortOrder.value = contentSortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    contentSortField.value = field
    contentSortOrder.value = 'desc'
  }
  loadPosts()
}

const isAllSelected = computed(() => {
  if (filteredContentPosts.value.length === 0) return false
  return filteredContentPosts.value.every(p => selectedPostIds.value.includes(p.id))
})

const toggleAll = () => {
  if (isAllSelected.value) {
    selectedPostIds.value = []
  } else {
    selectedPostIds.value = filteredContentPosts.value.map(p => p.id)
  }
}

const togglePost = (postId: string) => {
  const idx = selectedPostIds.value.indexOf(postId)
  if (idx === -1) {
    selectedPostIds.value = [...selectedPostIds.value, postId]
  } else {
    selectedPostIds.value = selectedPostIds.value.filter(id => id !== postId)
  }
}

const handlePin = async (postId: string) => {
  try {
    await pinPost(postId)
    ElMessage.success('置顶成功')
    selectedPostIds.value = []
    loadPosts()
  } catch (error: any) {
    ElMessage.error(error?.message || '置顶失败')
  }
}

const handleUnpin = async (postId: string) => {
  try {
    await unpinPost(postId)
    ElMessage.success('取消置顶成功')
    selectedPostIds.value = []
    loadPosts()
  } catch (error: any) {
    ElMessage.error(error?.message || '取消置顶失败')
  }
}

const handleBatchPublish = async () => {
  try {
    await ElMessageBox.confirm(`确认批量发布 ${selectedPostIds.value.length} 篇草稿？`, '批量发布', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    await batchOperate({ postIds: selectedPostIds.value, action: 'publish' })
    ElMessage.success('批量发布成功')
    selectedPostIds.value = []
    loadPosts()
  } catch (error: any) {
    if (error !== 'cancel') {
      const msg = error?.message || '批量发布失败'
      ElMessage.error(msg)
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确认批量删除 ${selectedPostIds.value.length} 篇作品？`, '批量删除', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await batchOperate({ postIds: selectedPostIds.value, action: 'delete' })
    ElMessage.success('批量删除成功')
    selectedPostIds.value = []
    loadPosts()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '批量删除失败')
    }
  }
}

const loadPosts = async () => {
  if (!userStore.userInfo?.id) return
  await run(async () => {
    const data = await getUserPosts({ page: 1, size: 100, sortField: contentSortField.value, sortOrder: contentSortOrder.value })
    posts.value = data.records || []
  }, { onError: () => ElMessage.error('加载失败') })
}

const refreshPosts = () => {
  if (!loading.value) {
    loadPosts()
  }
}

onMounted(() => {
  loadPosts()
})

defineExpose({
  refreshPosts,
  posts,
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

.header-left p {
  font-size: 14px;
  color: var(--text-dim);
}

.page-container .page-header h1 {
  font-size: 24px;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.publish-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  border-radius: 12px;
  background: var(--gradient-brand);
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

.publish-btn .btn-icon {
  width: 16px;
  height: 16px;
  color: white;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-group {
  display: flex;
  gap: 6px;
  background: var(--pink-bg);
  border-radius: 12px;
  padding: 4px;
}

.filter-btn {
  padding: 7px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  background: transparent;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.filter-btn:hover:not(.active) {
  background: var(--pink-bg-hover);
  color: var(--pink);
}

.filter-btn.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15), rgba(180, 132, 255, 0.15));
  color: var(--pink);
  box-shadow: none;
}

.filter-count {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 8px;
  background: rgba(127, 127, 127, 0.12);
  font-weight: 600;
  line-height: 1.6;
  display: inline-flex;
  align-items: center;
}

.filter-btn.active .filter-count {
  background: rgba(255, 107, 157, 0.25);
}

html.dark .filter-count {
  background: rgba(255, 255, 255, 0.08);
}

.refresh-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1.5px solid var(--border);
  background: var(--card);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease-out;
  flex-shrink: 0;
}

.refresh-btn:hover {
  border-color: var(--pink);
  background: var(--pink-bg);
}

.refresh-btn.spinning .refresh-icon {
  animation: spin 0.8s linear infinite;
}

.refresh-icon {
  width: 16px;
  height: 16px;
  opacity: 0.5;
  color: var(--text-dim);
  transition: opacity 0.22s ease-out;
}

.refresh-btn:hover .refresh-icon {
  opacity: 0.8;
  color: var(--pink);
}

.batch-toggle-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 10px;
  border: 1.5px solid var(--border);
  background: var(--card);
  cursor: pointer;
  font-size: 12px;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
  white-space: nowrap;
}

.batch-toggle-btn:hover {
  border-color: var(--pink);
  color: var(--pink);
}

.batch-toggle-btn.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  border-color: var(--pink);
  color: var(--pink);
}

.batch-toggle-icon {
  width: 15px;
  height: 15px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.search-bar {
  margin-top: 16px;
  margin-bottom: 20px;
}

.content-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.content-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--card);
  border: 1.5px solid var(--border);
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.04);
  transition: all 0.22s ease-out;
}

.content-item:hover {
  border-color: var(--border);
  box-shadow: 0 4px 16px rgba(180, 132, 255, 0.1);
  transform: translateY(-2px);
}

.content-cover {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.1);
}

.content-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.22s ease-out;
}

.content-cover.clickable {
  cursor: pointer;
}

.content-cover.clickable:hover img {
  transform: scale(1.05);
}

.content-info {
  flex: 1;
  min-width: 0;
}

.content-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 8px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  min-width: 0;
  transition: color 0.22s ease-out;
}

.content-title.clickable {
  cursor: pointer;
}

.content-title.clickable:hover {
  color: var(--pink);
}

.title-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
  color: inherit;
}

.content-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.content-time {
  font-size: 12px;
  color: var(--text-dim);
}

.content-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
  max-width: 160px;
}

.review-reason {
  font-size: 11px;
  color: var(--danger-color, #f56c6c);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.content-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: var(--gradient-card);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--text-dim);
}

.section-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  font-weight: 500;
}

.pin-tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.85), rgba(251, 146, 60, 0.85));
  color: var(--white);
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
  white-space: nowrap;
  box-shadow: 0 1px 4px rgba(245, 158, 11, 0.25);
}

.pin-tag .iconify {
  width: 12px;
  height: 12px;
}

.content-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-dim);
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-dim);
  line-height: 1;
}

.stat-mini-icon {
  width: 14px;
  height: 14px;
  display: block;
  flex-shrink: 0;
}

.status-tag {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 8px;
  font-weight: 500;
}

.status-0 {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.12), rgba(255, 152, 0, 0.12));
  color: var(--warning);
}

.status-1 {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12), rgba(37, 99, 235, 0.12));
  color: #3b82f6;
}

.status-2 {
  background: linear-gradient(135deg, rgba(76, 175, 80, 0.12), rgba(56, 142, 60, 0.12));
  color: var(--success);
}

.status-3 {
  background: linear-gradient(135deg, rgba(244, 67, 54, 0.12), rgba(211, 47, 47, 0.12));
  color: var(--error);
}

.status-4 {
  background: linear-gradient(135deg, rgba(158, 158, 158, 0.12), rgba(117, 117, 117, 0.12));
  color: #9e9e9e;
}

.action-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1.5px solid var(--border);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease-out;
  background: var(--card);
}

.action-btn:hover {
  transform: scale(1.06);
  border-color: transparent;
}

.action-btn .action-icon,
.action-btn .action-icon-svg {
  width: 14px;
  height: 14px;
}

.action-btn.edit:hover {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.12), rgba(180, 132, 255, 0.12));
  border-color: var(--border);
}

.action-btn.preview .action-icon-svg {
  color: #6366f1;
}

.action-btn.preview:hover {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(139, 92, 246, 0.1));
  border-color: rgba(99, 102, 241, 0.3);
}

.action-btn.publish:hover {
  background: linear-gradient(135deg, rgba(76, 175, 80, 0.1), rgba(56, 142, 60, 0.1));
  border-color: rgba(76, 175, 80, 0.3);
}

.action-btn.publish .action-icon {
  color: var(--success);
}

.action-btn.restore:hover {
  background: linear-gradient(135deg, rgba(33, 150, 243, 0.1), rgba(25, 118, 210, 0.1));
  border-color: rgba(33, 150, 243, 0.3);
}

.action-btn.restore .action-icon {
  color: #2196f3;
}

.action-btn.delete:hover {
  background: linear-gradient(135deg, rgba(244, 67, 54, 0.1), rgba(211, 47, 47, 0.1));
  border-color: rgba(244, 67, 54, 0.3);
}

.action-btn.delete .action-icon {
  color: var(--error);
}

.create-btn {
  padding: 12px 32px;
  border-radius: 25px;
  background: var(--gradient-brand);
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

.sort-arrow {
  font-size: 11px;
  line-height: 1;
}

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  margin-bottom: 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.08), rgba(180, 132, 255, 0.08));
  border: 1px solid rgba(255, 107, 157, 0.2);
}

.batch-count {
  font-size: 13px;
  font-weight: 600;
  color: var(--pink);
}

.batch-actions {
  display: flex;
  gap: 8px;
  flex: 1;
}

.batch-btn {
  padding: 5px 14px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.batch-btn.batch-publish {
  background: var(--success-bg);
  color: var(--success);
  border-color: rgba(76, 175, 80, 0.2);
}

.batch-btn.batch-publish:hover {
  background: var(--success-bg);
}

.batch-btn.batch-delete {
  background: var(--error-bg);
  color: var(--error);
  border-color: rgba(244, 67, 54, 0.2);
}

.batch-btn.batch-delete:hover {
  background: var(--error-bg);
}

.batch-clear {
  padding: 5px 12px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 12px;
  color: var(--text-dim);
  cursor: pointer;
}

.batch-clear:hover {
  color: var(--text);
}

/* 全选栏 */
.select-all-bar {
  padding: 4px 0 10px;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-dim);
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  accent-color: var(--pink);
}

/* 复选框 */
.content-checkbox {
  flex-shrink: 0;
}

.content-checkbox input[type="checkbox"] {
  accent-color: var(--pink);
  width: 16px;
  height: 16px;
}

/* 置顶按钮 */
.action-btn.pin .pin-icon {
  color: var(--pink);
}

.action-btn.pin:hover {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  border-color: rgba(255, 107, 157, 0.3);
}

.action-btn.unpin .unpin-icon {
  color: var(--warning);
}

.action-btn.unpin:hover {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(251, 191, 36, 0.1));
  border-color: rgba(245, 158, 11, 0.3);
}
</style>