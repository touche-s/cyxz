<template>
  <div class="received-list-wrap">
    <LoadingSpinner v-if="loading" text="加载中..." />
    <EmptyState v-else-if="!records.length" icon="ph:chat-circle-dots" title="还没有收到过评论" />
    <div v-else class="received-list">
      <div class="received-item" v-for="comment in records" :key="comment.id">
        <UserAvatar :src="comment.userAvatar" :name="comment.userName" :user-id="comment.userId" :size="36" />
        <div class="received-body">
          <div class="received-top">
            <span class="received-name clickable" @click="goUser(comment.userId)">{{ comment.userName || '用户' }}</span>
            <span class="received-time">{{ formatTime(comment.createTime) }}</span>
          </div>
          <div class="received-content">{{ comment.content }}</div>
          <div class="received-context">
            来自作品
            <span class="received-post-title" @click="goPost(comment.postId)">{{ comment.postTitle || `帖子 ${comment.postId}` }}</span>
            <template v-if="comment.replyToUserName">，回复了 @{{ comment.replyToUserName }}</template>
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
import { getReceivedComments } from '@/api/comment'
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
    const data = await getReceivedComments({ page: p, size: pageSize })
    records.value = data.records || []
    total.value = data.total || 0
    page.value = p
  } catch (error) {
    console.error('加载收到的评论失败:', error)
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
  align-items: flex-start;
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

.received-content {
  font-size: 14px;
  color: var(--text);
  line-height: 1.5;
  word-break: break-word;
}

.received-context {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 4px;
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
