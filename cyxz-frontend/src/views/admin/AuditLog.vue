<template>
  <section class="admin-section">
    <div class="section-head">
      <div>
        <h2>审计日志</h2>
        <p class="section-desc">共 {{ total }} 条记录</p>
      </div>
      <div class="section-head-right">
        <button class="toolbar-btn" @click="loadData" title="刷新"><Icon icon="ph:arrows-clockwise" /></button>
      </div>
    </div>
    <div class="section-search">
      <select v-model="actionFilter" class="filter-select" @change="page = 1; loadData()">
        <option value="">全部动作</option>
        <option value="USER_DISABLE">禁用用户</option>
        <option value="USER_ENABLE">启用用户</option>
        <option value="POST_DELETE">删除帖子</option>
        <option value="POST_APPROVE">通过帖子</option>
        <option value="POST_REJECT">拒绝帖子</option>
        <option value="REPORT_APPROVE">通过举报</option>
        <option value="REPORT_REJECT">驳回举报</option>
        <option value="CIRCLE_APPROVE">通过建圈</option>
        <option value="CIRCLE_REJECT">驳回建圈</option>
        <option value="CIRCLE_JOIN_APPROVE">通过入圈</option>
        <option value="CIRCLE_JOIN_REJECT">驳回入圈</option>
      </select>
      <input type="date" v-model="startDate" class="filter-select" @change="page = 1; loadData()" />
      <input type="date" v-model="endDate" class="filter-select" @change="page = 1; loadData()" />
      <SearchInput v-model="searchKeyword" variant="inline" placeholder="搜索操作人或详情..." />
    </div>
    <LoadingSpinner v-if="loading && list.length === 0" />
    <div v-else class="table-wrap">
      <table v-if="filteredList.length > 0" class="data-table">
        <thead>
          <tr>
            <th style="width:80px">ID</th>
            <th style="width:120px">操作人</th>
            <th style="width:120px">动作</th>
            <th style="width:80px">对象类型</th>
            <th style="width:80px">对象ID</th>
            <th style="width:140px">IP</th>
            <th>详情</th>
            <th style="width:110px">时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in filteredList" :key="a.id">
            <td class="td-id">{{ a.id }}</td>
            <td class="td-name">{{ a.operatorName }}</td>
            <td><span class="tag tag-blue">{{ actionLabel(a.action) }}</span></td>
            <td><span class="tag tag-gray">{{ a.targetType }}</span></td>
            <td class="td-id">{{ a.targetId }}</td>
            <td style="font-family:monospace;font-size:12px;color:var(--text-dim)">{{ a.ip || '-' }}</td>
            <td class="td-intro" :title="a.detail">{{ a.detail?.slice(0, 30) || '-' }}</td>
            <td class="td-time">{{ a.createTime?.slice(0, 16).replace('T', ' ') }}</td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="filteredList.length === 0" title="暂无审计日志" />
    </div>
    <div v-if="total > 10" class="pagination">
      <button class="page-btn" :disabled="page === 1" @click="page--; loadData()">上一页</button>
      <span class="page-info">第 {{ page }} 页 / 共 {{ Math.ceil(total / 10) }} 页</span>
      <button class="page-btn" :disabled="list.length < 10" @click="page++; loadData()">下一页</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import SearchInput from '@/components/SearchInput.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getAuditLogList, type AuditLogVO } from '@/api/audit'

const props = defineProps<{ searchKeyword: string }>()
const emit = defineEmits<{ (e: 'update:searchKeyword', v: string): void }>()

const searchKeyword = computed({
  get: () => props.searchKeyword,
  set: (v) => emit('update:searchKeyword', v)
})

const list = ref<AuditLogVO[]>([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const actionFilter = ref('')
const startDate = ref('')
const endDate = ref('')

const actionLabelMap: Record<string, string> = {
  USER_DISABLE: '禁用用户', USER_ENABLE: '启用用户',
  POST_DELETE: '删除帖子', POST_APPROVE: '通过帖子', POST_REJECT: '拒绝帖子',
  REPORT_APPROVE: '通过举报', REPORT_REJECT: '驳回举报',
  CIRCLE_APPROVE: '通过建圈', CIRCLE_REJECT: '驳回建圈',
  CIRCLE_JOIN_APPROVE: '通过入圈', CIRCLE_JOIN_REJECT: '驳回入圈'
}

function actionLabel(a: string) { return actionLabelMap[a] || a }

const filteredList = computed(() => {
  const q = searchKeyword.value.toLowerCase().trim()
  if (!q) return list.value
  return list.value.filter(a =>
    a.operatorName?.toLowerCase().includes(q) ||
    a.detail?.toLowerCase().includes(q) ||
    a.action?.toLowerCase().includes(q)
  )
})

async function loadData() {
  loading.value = true
  try {
    const res = await getAuditLogList({
      action: actionFilter.value || undefined,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined,
      page: page.value,
      size: 10
    })
    list.value = res.records || []
    total.value = res.total || 0
  } catch { list.value = [] }
  finally { loading.value = false }
}

onMounted(loadData)
</script>
