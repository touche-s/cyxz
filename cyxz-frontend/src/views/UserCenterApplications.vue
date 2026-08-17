<template>
  <div class="app-page-container">
    <div class="app-tabs">
      <button
        v-for="t in appTabs"
        :key="t.key"
        class="app-tab"
        :class="{ active: appTab === t.key }"
        @click="switchTab(t.key)"
      >
        <Icon :icon="t.icon" class="app-tab-icon" />
        {{ t.label }}
      </button>
    </div>

    <LoadingSpinner v-if="loading" text="加载中..." />
    <EmptyState v-else-if="!records.length" icon="ph:document-text" title="暂无相关记录" />
    <div v-else class="app-list">
      <div class="app-item" v-for="item in records" :key="item.id">
        <div class="app-item-main">
          <div class="app-item-title-row">
            <span class="app-item-title">{{ itemTitle(item) }}</span>
            <span class="app-status" :class="statusCls(item.status)">{{ statusLabel(item.status) }}</span>
          </div>
          <p class="app-item-desc">{{ itemDesc(item) }}</p>
          <p v-if="item.handlerNote" class="app-item-note">处理备注：{{ item.handlerNote }}</p>
        </div>
        <span class="app-item-time">{{ formatTime(item.createTime) }}</span>
      </div>
    </div>
    <Pagination v-if="total > pageSize" :current="page" :total="total" :page-size="pageSize" @change="load" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'
import { getMyReports } from '@/api/governance'
import { getMyCircleApplications, getMyCircleJoinApplications } from '@/api/circle'
import { formatTime } from '@/utils/format'

type AppTabKey = 'report' | 'circle' | 'join'

const appTabs = [
  { key: 'report' as AppTabKey, label: '我的举报', icon: 'ph:flag' },
  { key: 'circle' as AppTabKey, label: '建圈申请', icon: 'ph:users-three' },
  { key: 'join' as AppTabKey, label: '入圈申请', icon: 'ph:user-plus' },
]

const appTab = ref<AppTabKey>('report')
const records = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const statusMap: Record<string, { label: string; cls: string }> = {
  PENDING: { label: '待处理', cls: 'st-pending' },
  APPROVED: { label: '已通过', cls: 'st-approved' },
  REJECTED: { label: '已驳回', cls: 'st-rejected' },
}

const statusLabel = (s: string) => statusMap[s]?.label || s || '未知'
const statusCls = (s: string) => statusMap[s]?.cls || ''

const itemTitle = (item: any): string => {
  if (appTab.value === 'report') return item.targetType === 'POST' ? '举报帖子' : '举报评论'
  if (appTab.value === 'circle') return item.name || `建圈申请 #${item.id}`
  return `入圈申请 #${item.id}`
}

const itemDesc = (item: any): string => {
  if (appTab.value === 'report') return `原因：${item.reason || '无'}`
  if (appTab.value === 'circle') return item.intro || '暂无简介'
  return `目标圈子 #${item.circleId}` + (item.reason ? `，理由：${item.reason}` : '')
}

const switchTab = (key: AppTabKey) => {
  appTab.value = key
  page.value = 1
  load()
}

const load = async (p = 1) => {
  loading.value = true
  try {
    const params = { page: p, size: pageSize }
    let data: any
    if (appTab.value === 'report') {
      data = await getMyReports(params)
    } else if (appTab.value === 'circle') {
      data = await getMyCircleApplications(params)
    } else {
      data = await getMyCircleJoinApplications(params)
    }
    records.value = data?.records || []
    total.value = data?.total || 0
    page.value = p
  } catch (error) {
    console.error('加载申请记录失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.app-page-container {
  min-height: 300px;
}

.app-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.app-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1.5px solid var(--border);
  background: var(--card);
  color: var(--text-dim);
  font-size: var(--fs-md);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.app-tab:hover {
  border-color: var(--brand-purple, #9b6dff);
  color: var(--brand-purple, #9b6dff);
}

.app-tab.active {
  background: var(--gradient-brand, linear-gradient(135deg, #ff8fb8, #9b6dff));
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.25);
}

.app-tab-icon {
  font-size: 15px;
}

.app-list {
  display: flex;
  flex-direction: column;
}

.app-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--border);
}

.app-item:last-child {
  border-bottom: none;
}

.app-item-main {
  flex: 1;
  min-width: 0;
}

.app-item-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.app-item-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--text);
}

.app-status {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 999px;
}

.st-pending {
  color: #b88230;
  background: rgba(250, 173, 20, 0.12);
}

.st-approved {
  color: #2f9e6e;
  background: rgba(72, 199, 142, 0.12);
}

.st-rejected {
  color: #d54949;
  background: rgba(245, 108, 108, 0.12);
}

.app-item-desc {
  font-size: 13px;
  color: var(--text-secondary, #666);
  line-height: 1.5;
  word-break: break-word;
}

.app-item-note {
  font-size: var(--fs-xs);
  color: var(--text-dim);
  margin-top: 4px;
}

.app-item-time {
  flex-shrink: 0;
  font-size: var(--fs-xs);
  color: var(--text-dim);
}
</style>
