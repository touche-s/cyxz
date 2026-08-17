<template>
  <div class="received-list-wrap">
    <LoadingSpinner v-if="loading" text="加载中..." />
    <EmptyState v-else-if="!records.length" icon="ph:heart-straight" title="还没有收到过赞" />
    <div v-else class="received-list">
      <div class="received-item" v-for="item in records" :key="item.likeId">
        <UserAvatar :src="item.userAvatar" :name="item.userName" :user-id="item.userId" :size="36" />
        <div class="received-body">
          <div class="received-top">
            <span class="received-name clickable" @click="goUser(item.userId)">{{ item.userName || '用户' }}</span>
            <span class="received-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="received-desc">
            赞了你的作品
            <span class="received-post-title" @click="goPost(item.postId)">{{ item.postTitle || `帖子 ${item.postId}` }}</span>
          </div>
        </div>
      </div>
    </div>
    <Pagination v-if="total > pageSize" :current="page" :total="total" :page-size="pageSize" @change="load" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UserAvatar from '@/components/UserAvatar.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'
import { getReceivedLikes } from '@/api/post'
import { formatTime } from '@/utils/format'
import { useNavigate } from '@/composables/useNavigate'

const { open } = useNavigate()

const records = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const load = async (p = 1) => {
  loading.value = true
  try {
    const data = await getReceivedLikes({ page: p, size: pageSize })
    records.value = data.records || []
    total.value = data.total || 0
    page.value = p
  } catch (error) {
    console.error('加载收到的赞失败:', error)
  } finally {
    loading.value = false
  }
}

const goUser = (userId: string | number) => {
  if (userId) open(`/user/${userId}`)
}

const goPost = (postId: string | number) => {
  open(`/post/${postId}`)
}

onMounted(() => load())
</script>

<style scoped>
.received-list-wrap {
  min-height: 200px;
}

.received-list {
  display: flex;
  flex-direction: column;
}

.received-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--border);
}

.received-item:last-child {
  border-bottom: none;
}

.received-body {
  flex: 1;
  min-width: 0;
}

.received-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.received-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text);
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  color: var(--brand-pink, #ff8fb8);
}

.received-time {
  font-size: 12px;
  color: var(--text-dim);
}

.received-desc {
  font-size: 13px;
  color: var(--text-secondary, #666);
}

.received-post-title {
  color: var(--brand-purple, #9b6dff);
  cursor: pointer;
  margin-left: 2px;
}

.received-post-title:hover {
  text-decoration: underline;
}
</style>
