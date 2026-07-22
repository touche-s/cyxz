<template>
  <div class="page-container">
    <header class="page-header">
      <div class="header-left">
        <h1>内容管理</h1>
        <p>管理你的所有作品</p>
      </div>
      <button class="publish-btn" @click="$emit('createPost')">
        <Icon icon="ph:pencil-simple" class="btn-icon" />
        <span>发布新作品</span>
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
      <SearchInput v-model="searchKeyword" variant="inline" placeholder="搜索当前分类下的作品标题..." />
    </div>

    <div class="content-list" v-if="!loading && filteredContentPosts.length > 0">
      <div class="content-item" v-for="post in filteredContentPosts" :key="post.id">
        <div class="content-cover" :class="{ clickable: isPublished(post.status) }" @click="isPublished(post.status) && $emit('view', post.id)">
          <img v-if="post.cover" :src="post.cover" alt="cover" />
          <div v-else class="cover-placeholder">
            <span>暂无封面</span>
          </div>
        </div>
        <div class="content-info">
          <h3 class="content-title" :class="{ clickable: isPublished(post.status) }" @click="isPublished(post.status) && $emit('view', post.id)">{{ post.title }}</h3>
          <div class="content-meta">
            <span class="category-tag" v-if="post.categoryName">{{ post.categoryName }}</span>
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
        </div>
        <div class="content-actions">
          <button v-if="!isDeleted(post.status)" class="action-btn edit" @click="$emit('edit', post.id)" title="编辑">
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

    <EmptyState v-else-if="!loading" :icon="iconEmpty" :title="`还没有${activeContentTab === 'published' ? '已发布的' : activeContentTab === 'draft' ? '草稿' : activeContentTab === 'deleted' ? '已删除的' : ''}作品`">
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
import { getUserPosts } from '@/api/post'
import { formatDateTime } from '@/utils/format'
import type { PostVO } from '@/api/post'
import { isDraft, isPublished, isDeleted, statusText } from '@/utils/postStatus'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

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
const loading = ref(false)
const activeContentTab = ref<'all' | 'published' | 'draft' | 'deleted'>('all')
const searchKeyword = ref('')
const contentSortField = ref('create_time')
const contentSortOrder = ref('desc')

const iconEmpty = 'ph:tray'

const contentTabs = computed(() => {
  const activePosts = posts.value.filter(p => !isDeleted(p.status))
  return [
    { label: '全部', value: 'all' as const, count: activePosts.length },
    { label: '已发布', value: 'published' as const, count: posts.value.filter(p => isPublished(p.status)).length },
    { label: '草稿', value: 'draft' as const, count: posts.value.filter(p => isDraft(p.status)).length },
    { label: '已删除', value: 'deleted' as const, count: posts.value.filter(p => isDeleted(p.status)).length },
  ]
})

const filteredContentPosts = computed(() => {
  let filtered = posts.value

  switch (activeContentTab.value) {
    case 'published':
      filtered = filtered.filter(p => isPublished(p.status))
      break
    case 'draft':
      filtered = filtered.filter(p => isDraft(p.status))
      break
    case 'deleted':
      filtered = filtered.filter(p => isDeleted(p.status))
      break
    default:
      filtered = filtered.filter(p => !isDeleted(p.status))
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

const loadPosts = async () => {
  if (!userStore.userInfo?.id) return
  loading.value = true
  try {
    const data = await getUserPosts({ page: 1, size: 100, sortField: contentSortField.value, sortOrder: contentSortOrder.value })
    posts.value = data.records || []
  } catch (error) {
    console.error('加载帖子失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
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
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.publish-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  border-radius: 12px;
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
  background: rgba(0, 0, 0, 0.06);
  font-weight: 600;
  line-height: 1.6;
  display: inline-flex;
  align-items: center;
}

.filter-btn.active .filter-count {
  background: rgba(255, 255, 255, 0.3);
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

@keyframes spin {
  to { transform: rotate(360deg); }
}

.search-bar {
  margin-top: 16px;
  margin-bottom: 20px;
  max-width: 360px;
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.22s ease-out;
}

.content-title.clickable {
  cursor: pointer;
}

.content-title.clickable:hover {
  color: var(--pink);
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
  flex-shrink: 0;
}

.content-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
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

.category-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  font-weight: 500;
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
  vertical-align: middle;
}

.status-tag {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 8px;
  font-weight: 500;
}

.status-0 {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.12), rgba(255, 152, 0, 0.12));
  color: #f57c00;
}

.status-1 {
  background: linear-gradient(135deg, rgba(76, 175, 80, 0.12), rgba(56, 142, 60, 0.12));
  color: #2e7d32;
}

.status-2 {
  background: linear-gradient(135deg, rgba(244, 67, 54, 0.12), rgba(211, 47, 47, 0.12));
  color: #c62828;
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
  color: #4caf50;
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
  color: #f44336;
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
</style>
