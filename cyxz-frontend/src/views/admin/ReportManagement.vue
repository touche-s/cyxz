<template>
  <section class="admin-section">
    <div class="section-head">
      <div>
        <h2>举报管理</h2>
        <p class="section-desc">共 {{ total }} 条举报</p>
      </div>
      <div class="section-head-right">
        <button class="toolbar-btn" @click="loadData" title="刷新"><Icon icon="ph:arrows-clockwise" /></button>
      </div>
    </div>
    <div class="section-search">
      <select v-model="statusFilter" class="filter-select" @change="page = 1; loadData()">
        <option value="">全部状态</option>
        <option value="PENDING">待处理</option>
        <option value="APPROVED">已通过</option>
        <option value="REJECTED">已驳回</option>
      </select>
      <select v-model="targetTypeFilter" class="filter-select" @change="page = 1; loadData()">
        <option value="">全部类型</option>
        <option value="POST">帖子</option>
        <option value="COMMENT">评论</option>
      </select>
      <SearchInput v-model="searchKeyword" variant="inline" placeholder="搜索举报原因..." />
    </div>
    <LoadingSpinner v-if="loading && list.length === 0" />
    <div v-else class="table-wrap">
      <table v-if="filteredList.length > 0" class="data-table">
        <thead>
          <tr>
            <th style="width:80px">ID</th>
            <th style="width:90px">类型</th>
            <th style="width:80px">对象ID</th>
            <th>举报原因</th>
            <th style="width:80px">状态</th>
            <th style="width:100px">举报时间</th>
            <th style="width:160px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in filteredList" :key="r.id">
            <td class="td-id">{{ r.id }}</td>
            <td><span class="tag tag-blue">{{ r.targetType === 'POST' ? '帖子' : '评论' }}</span></td>
            <td class="td-id">{{ r.targetId }}</td>
            <td class="td-intro" :title="r.reason">{{ r.reason?.slice(0, 40) || '-' }}</td>
            <td><span class="tag" :class="statusTagClass[r.status]">{{ statusLabel(r.status) }}</span></td>
            <td class="td-time">{{ r.createTime?.slice(0, 10) }}</td>
            <td class="td-actions" v-if="r.status === 'PENDING'">
              <button class="op-link op-link-green" @click="openReview(r, 'APPROVED')">通过</button>
              <button class="op-link op-link-danger" @click="openReview(r, 'REJECTED')">驳回</button>
            </td>
            <td v-else class="td-time">{{ r.handlerNote?.slice(0, 20) || '已处理' }}</td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="filteredList.length === 0" title="暂无举报记录" />
    </div>
    <div v-if="total > 10" class="pagination">
      <button class="page-btn" :disabled="page === 1" @click="page--; loadData()">上一页</button>
      <span class="page-info">第 {{ page }} 页 / 共 {{ Math.ceil(total / 10) }} 页</span>
      <button class="page-btn" :disabled="list.length < 10" @click="page++; loadData()">下一页</button>
    </div>

    <Teleport to="body">
      <div class="modal-overlay" v-if="reviewTarget" @click.self="reviewTarget = null">
        <div class="modal-card" style="width:420px">
          <div class="modal-header">
            <h3>{{ actionType === 'APPROVED' ? '通过' : '驳回' }}举报</h3>
            <button class="modal-close" @click="reviewTarget = null">&times;</button>
          </div>
          <div class="modal-body">
            <p class="modal-hint">举报原因：{{ reviewTarget.reason }}</p>
            <div class="form-row">
              <label>处理意见</label>
              <textarea v-model="reviewNote" class="form-textarea" rows="3" placeholder="请输入处理意见"></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="reviewTarget = null">取消</button>
            <button class="submit-btn" :class="{ 'op-btn-danger-bg': actionType === 'REJECTED' }" @click="submitReview" :disabled="submitting">
              {{ submitting ? '提交中...' : '确认' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import SearchInput from '@/components/SearchInput.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getReportList, approveReport, rejectReport, type ReportVO } from '@/api/governance'

const props = defineProps<{ searchKeyword: string }>()
const emit = defineEmits<{ (e: 'update:searchKeyword', v: string): void }>()

const searchKeyword = computed({
  get: () => props.searchKeyword,
  set: (v) => emit('update:searchKeyword', v)
})

const list = ref<ReportVO[]>([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const statusFilter = ref('')
const targetTypeFilter = ref('')

const statusTagClass: Record<string, string> = {
  PENDING: 'tag-amber', APPROVED: 'tag-green', REJECTED: 'tag-red'
}

function statusLabel(s: string) {
  if (s === 'PENDING') return '待处理'
  if (s === 'APPROVED') return '已通过'
  if (s === 'REJECTED') return '已驳回'
  return s
}

const filteredList = computed(() => {
  const q = searchKeyword.value.toLowerCase().trim()
  if (!q) return list.value
  return list.value.filter(r => r.reason?.toLowerCase().includes(q))
})

const reviewTarget = ref<ReportVO | null>(null)
const actionType = ref('APPROVED')
const reviewNote = ref('')
const submitting = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await getReportList({
      status: statusFilter.value || undefined,
      targetType: targetTypeFilter.value || undefined,
      page: page.value,
      size: 10
    })
    list.value = res.records || []
    total.value = res.total || 0
  } catch { list.value = [] }
  finally { loading.value = false }
}

function openReview(r: ReportVO, action: string) {
  reviewTarget.value = r
  actionType.value = action
  reviewNote.value = ''
}

async function submitReview() {
  if (!reviewTarget.value) return
  if (!reviewNote.value.trim()) { ElMessage.warning('请填写处理意见'); return }
  submitting.value = true
  try {
    if (actionType.value === 'APPROVED') {
      await approveReport(reviewTarget.value.id, reviewNote.value.trim())
    } else {
      await rejectReport(reviewTarget.value.id, reviewNote.value.trim())
    }
    ElMessage.success('处理成功')
    reviewTarget.value = null
    await loadData()
  } catch { ElMessage.error('操作失败') }
  finally { submitting.value = false }
}

watch(() => [statusFilter.value, targetTypeFilter.value], () => { page.value = 1; loadData() })
onMounted(loadData)
</script>
