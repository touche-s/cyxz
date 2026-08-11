<template>
  <section class="admin-section">
    <div class="section-head">
      <div>
        <h2>数据看板</h2>
        <p class="section-desc">近 {{ trendDays }} 天的平台数据概览</p>
      </div>
      <div class="section-head-right">
        <select v-model="trendDays" class="filter-select" @change="loadTrend(currentMetric)">
          <option :value="7">近 7 天</option>
          <option :value="14">近 14 天</option>
          <option :value="30">近 30 天</option>
        </select>
        <button class="toolbar-btn" @click="loadAll" title="刷新"><Icon icon="ph:arrows-clockwise" /></button>
      </div>
    </div>

    <LoadingSpinner v-if="loading" text="加载中..." />
    <template v-else>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-value">{{ dashboard?.newUsers ?? '-' }}</div>
          <div class="stat-label">新增用户</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ dashboard?.newPosts ?? '-' }}</div>
          <div class="stat-label">新增帖子</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ (dashboard?.approvedPosts ?? 0) + (dashboard?.rejectedPosts ?? 0) }}</div>
          <div class="stat-label">审核帖子</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ dashboard?.newCircles ?? '-' }}</div>
          <div class="stat-label">新建圈子</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ dashboard?.newJoins ?? '-' }}</div>
          <div class="stat-label">新加入圈子</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ dashboard?.reportHandled ?? '-' }}</div>
          <div class="stat-label">处理举报</div>
        </div>
      </div>

      <div class="chart-section">
        <div class="chart-tabs">
          <button
            v-for="m in metrics"
            :key="m.key"
            class="chart-tab"
            :class="{ active: currentMetric === m.key }"
            @click="loadTrend(m.key)"
          >{{ m.label }}</button>
        </div>
        <v-chart :option="chartOption" style="height:320px" autoresize />
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Icon } from '@iconify/vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { getDashboard, getTrend, type DashboardVO, type TrendVO } from '@/api/analytics'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

defineProps<{ searchKeyword: string }>()

const dashboard = ref<DashboardVO | null>(null)
const loading = ref(false)
const trendDays = ref(7)
const currentMetric = ref('NEW_USER')
const trendData = ref<TrendVO[]>([])

const metrics = [
  { key: 'NEW_USER', label: '新增用户' },
  { key: 'NEW_POST', label: '新增帖子' },
  { key: 'POST_APPROVED', label: '审核通过' },
  { key: 'POST_REJECTED', label: '审核拒绝' },
  { key: 'NEW_CIRCLE', label: '新建圈子' },
  { key: 'NEW_JOIN', label: '新加入' },
  { key: 'REPORT_HANDLED', label: '处理举报' },
]

const currentMetricLabel = computed(() => metrics.find(m => m.key === currentMetric.value)?.label || '')

const chartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: trendData.value.map(t => t.date),
    axisLine: { lineStyle: { color: '#999' } }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLine: { lineStyle: { color: '#999' } }
  },
  series: [{
    name: currentMetricLabel.value,
    type: 'line',
    data: trendData.value.map(t => t.value),
    smooth: true,
    lineStyle: { color: '#ff6b9d', width: 2 },
    itemStyle: { color: '#ff6b9d' },
    areaStyle: { color: 'rgba(255,107,157,0.1)' }
  }]
}))

async function loadAll() {
  loading.value = true
  try {
    dashboard.value = await getDashboard()
    await loadTrend(currentMetric.value)
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function loadTrend(metric: string) {
  currentMetric.value = metric
  try {
    trendData.value = await getTrend({ metric, days: trendDays.value })
  } catch { trendData.value = [] }
}

onMounted(loadAll)
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 20px;
  text-align: center;
  transition: border-color 0.2s;
}

.stat-card:hover { border-color: var(--pink); }

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--pink);
  margin-bottom: 6px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-dim);
}

.chart-section {
  background: var(--card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 20px;
}

.chart-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.chart-tab {
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-dim);
  transition: all 0.2s;
}

.chart-tab:hover { border-color: var(--pink); color: var(--pink); }
.chart-tab.active {
  background: var(--gradient-brand);
  color: var(--white);
  border-color: transparent;
}
</style>
