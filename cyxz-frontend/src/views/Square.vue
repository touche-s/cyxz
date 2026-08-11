<template>
  <main class="main-content">
    <div class="page-inner">

      <!-- 页面标题 -->
      <div class="page-title">
        <h1>圈子</h1>
        <p>找到你喜欢的作品圈子，加入同好</p>
        <button class="create-circle-btn" @click="showCreateModal = true">
          <Icon icon="ph:plus" />
          申请建圈
        </button>
      </div>

      <!-- 搜索 -->
      <div class="search-bar">
        <Icon icon="ph:magnifying-glass" class="search-icon" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索圈子名 / 作品名"
          class="search-input"
        />
      </div>

      <!-- Tab 切换 -->
      <UnderlineTabs v-model="activeTab" :tabs="tabs" />

      <!-- 圈子列表 -->
      <LoadingSpinner v-if="loading" text="加载中..." />

      <template v-else>
        <div class="circle-grid" v-if="filteredCircles.length > 0">
          <CircleCard
            v-for="circle in filteredCircles"
            :key="circle.id"
            :circle="circle"
            :variant="activeTab === 'joined' ? 'joined' : undefined"
            @click="enterCircle(circle)"
            @toggle="toggleJoin(circle)"
          />
        </div>

        <EmptyState v-else icon="ph:circles-four" title="暂无圈子" :hint="emptyDesc" />
      </template>

      <!-- 申请建圈弹窗 -->
      <Teleport to="body">
        <div class="modal-overlay" v-if="showCreateModal" @click.self="showCreateModal = false">
          <div class="modal-card" style="width:460px">
            <div class="modal-header">
              <h3>申请建圈</h3>
              <button class="modal-close" @click="showCreateModal = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="form-row">
                <label>圈子名称 <span class="required">*</span></label>
                <input v-model="createForm.name" class="form-input" placeholder="输入圈子名称" maxlength="30" />
              </div>
              <div class="form-row">
                <label>圈子简介</label>
                <textarea v-model="createForm.intro" class="form-textarea" rows="2" placeholder="一句话简介（选填）" maxlength="100"></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button class="cancel-btn" @click="showCreateModal = false">取消</button>
              <button class="submit-btn" @click="submitCreateApplication" :disabled="createSubmitting">
                {{ createSubmitting ? '提交中...' : '提交申请' }}
              </button>
            </div>
          </div>
        </div>
      </Teleport>

    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { getCircleList, getJoinedCircles, getHotCircles, joinCircle, leaveCircle, submitCircleApplication } from '@/api/circle'
import type { CircleVO } from '@/api/circle'
import { useNavigate } from '@/composables/useNavigate'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import CircleCard from '@/components/CircleCard.vue'
import UnderlineTabs from '@/components/UnderlineTabs.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const { open } = useNavigate()
const { requireLogin } = useAuth()

const tabs = [
  { key: 'joined', label: '我的圈子', icon: 'ph:heart-straight' },
  { key: 'hot', label: '热门圈子', icon: 'ph:fire' },
  { key: 'all', label: '全部圈子', icon: 'ph:planet' },
]

const activeTab = ref('hot')
const circles = ref<CircleVO[]>([])
const joinedCircles = ref<CircleVO[]>([])
const hotCircles = ref<CircleVO[]>([])
const keyword = ref('')
const loading = ref(false)
const joinLoading = ref<Record<number, boolean>>({})
const showCreateModal = ref(false)
const createForm = ref({ name: '', intro: '' })
const createSubmitting = ref(false)

const filteredCircles = computed(() => {
  let list: CircleVO[] = []
  if (activeTab.value === 'joined') {
    list = joinedCircles.value
  } else if (activeTab.value === 'hot') {
    list = hotCircles.value
  } else {
    list = circles.value
  }
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list
  return list.filter(
    c => c.name.toLowerCase().includes(kw) || c.intro.toLowerCase().includes(kw)
  )
})

const emptyDesc = computed(() => {
  if (keyword.value.trim()) return '没有匹配的圈子，试试其他关键词'
  if (activeTab.value === 'joined') return '你还没有加入任何圈子'
  return '暂无圈子'
})

watch(activeTab, (key) => {
  router.replace({ query: { tab: key } })
  loadTabData(key)
})

async function loadTabData(key: string) {
  if (key === 'joined' && joinedCircles.value.length === 0) {
    await loadJoined()
  } else if (key === 'hot' && hotCircles.value.length === 0) {
    await loadHot()
  } else if (key === 'all' && circles.value.length === 0) {
    await loadAll()
  }
}

async function submitCreateApplication() {
  if (!createForm.value.name.trim()) { ElMessage.warning('请输入圈子名称'); return }
  createSubmitting.value = true
  try {
    await submitCircleApplication({ name: createForm.value.name.trim(), intro: createForm.value.intro || undefined })
    ElMessage.success('建圈申请已提交，请等待审核')
    showCreateModal.value = false
    createForm.value = { name: '', intro: '' }
  } catch { ElMessage.error('提交失败') }
  finally { createSubmitting.value = false }
}

async function loadAll() {
  loading.value = true
  try {
    circles.value = await getCircleList()
  } catch (e) {
    console.error('加载圈子失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadJoined() {
  loading.value = true
  try {
    joinedCircles.value = await getJoinedCircles()
  } catch (e) {
    console.error('加载我的圈子失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadHot() {
  loading.value = true
  try {
    const data = await getHotCircles()
    hotCircles.value = (data as CircleVO[]) || []
  } catch (e) {
    console.error('加载热门圈子失败:', e)
  } finally {
    loading.value = false
  }
}

function enterCircle(circle: CircleVO) {
  open(`/circle/${circle.id}`)
}

async function toggleJoin(circle: CircleVO) {
  if (!requireLogin()) return
  if (joinLoading.value[circle.id]) return
  const prevJoined = circle.joined
  const prevMemberCount = circle.memberCount
  joinLoading.value[circle.id] = true
  try {
    if (circle.joined) {
      await leaveCircle(circle.id)
      circle.joined = false
      circle.memberCount = Math.max(circle.memberCount - 1, 0)
      joinedCircles.value = joinedCircles.value.filter(c => c.id !== circle.id)
    } else {
      await joinCircle(circle.id)
      circle.joined = true
      circle.memberCount = circle.memberCount + 1
      if (!joinedCircles.value.find(c => c.id === circle.id)) {
        joinedCircles.value.push({ ...circle })
      }
    }
    // 同步全部列表中的加入状态
    const inAll = circles.value.find(c => c.id === circle.id)
    if (inAll) inAll.joined = circle.joined
    const inHot = hotCircles.value.find(c => c.id === circle.id)
    if (inHot) inHot.joined = circle.joined
  } catch (e) {
    console.error('操作失败:', e)
    circle.joined = prevJoined
    circle.memberCount = prevMemberCount
  } finally {
    joinLoading.value[circle.id] = false
  }
}

onMounted(() => {
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery && ['joined', 'hot', 'all'].includes(tabFromQuery)) {
    activeTab.value = tabFromQuery
  }
  loadTabData(activeTab.value)
})

// 监听外部 query 变化（首页"查看更多"跳转）
watch(() => route.query.tab, (val) => {
  if (val && ['joined', 'hot', 'all'].includes(val as string)) {
    activeTab.value = val as string
    loadTabData(activeTab.value)
  }
})
</script>

<style scoped>
.main-content {
  padding: 90px 0 60px;
}

.page-inner {
  width: min(1368px, calc(100vw - 48px));
  margin: 0 auto;
  padding: 0;
}

/* ===== Page Title ===== */
.page-title {
  text-align: center;
  margin-bottom: 28px;
}

.page-title h1 {
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
  margin: 0 0 6px 0;
}

.page-title p {
  font-size: 14px;
  color: var(--text-dim);
  margin: 0;
}

.create-circle-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 20px;
  border-radius: 10px;
  background: var(--gradient-brand);
  color: var(--white);
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.create-circle-btn:hover { opacity: 0.9; transform: translateY(-1px); }

/* ===== Search ===== */
.search-bar {
  margin-bottom: 20px;
}

/* ===== Circle Grid ===== */
.circle-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

@media (max-width: 1200px) {
  .circle-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 900px) {
  .circle-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .circle-grid { grid-template-columns: 1fr; }
  .main-content { padding: 90px 0 60px; }
}
</style>
