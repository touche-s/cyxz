<template>
  <main class="main-content">
    <div class="page-inner">

      <!-- 页面标题 -->
      <div class="page-title">
        <h1>圈子广场</h1>
        <p>找到你喜欢的作品圈子，加入同好</p>
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

        <EmptyState v-else title="暂无圈子" :description="emptyDesc" />
      </template>

    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { getCircleList, getJoinedCircles, getHotCircles, joinCircle, leaveCircle } from '@/api/circle'
import type { CircleVO } from '@/api/circle'
import { useNavigate } from '@/composables/useNavigate'
import { useAuth } from '@/composables/useAuth'
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

/* ===== Search ===== */
.search-bar {
  position: relative;
  max-width: 400px;
  margin: 0 auto 24px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: var(--text-dim);
}

.search-input {
  width: 100%;
  padding: 10px 16px 10px 40px;
  border-radius: 12px;
  border: 1.5px solid var(--border);
  font-size: 14px;
  color: var(--text);
  background: var(--card);
  transition: border-color 0.22s ease-out;
}

.search-input:focus {
  outline: none;
  border-color: var(--pink);
}

.search-input::placeholder {
  color: var(--text-dim);
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
