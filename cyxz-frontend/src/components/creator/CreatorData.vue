<template>
  <div class="creator-data">
    <h2 class="page-title">数据中心</h2>
    <p class="page-desc">基于已发布作品的统计趋势、分类分布与排行分析</p>

    <!-- 趋势分析 -->
    <div class="section-block">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">趋势分析</h3>
          <div class="granularity-tabs">
            <button class="tab-btn" :class="{ active: granularity === 'daily' }" @click="granularity = 'daily'">近30天</button>
            <button class="tab-btn" :class="{ active: granularity === 'monthly' }" @click="granularity = 'monthly'">按月</button>
          </div>
        </div>
        <v-chart v-if="trendChartOption" :option="trendChartOption" :autoresize="true" class="chart chart-trend" />
        <div class="chart-insight" v-if="trendInsight">
          <Icon icon="ph:lightbulb" class="insight-icon" />
          <span>{{ trendInsight }}</span>
        </div>
        <div v-if="!trendChartOption" class="chart-empty">
          <Icon icon="ph:chart-line" class="empty-icon" width="36" height="36" />
          <span>发布作品后，趋势数据会在这里呈现</span>
        </div>
      </div>
    </div>

    <!-- 分布与排行 -->
    <div class="section-block">
      <div class="charts-row">
        <!-- 分类分布 -->
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">分类分布</h3>
          </div>
          <v-chart v-if="pieChartOption" :option="pieChartOption" :autoresize="true" class="chart" />
          <div class="chart-insight" v-if="bestCategory">
            <Icon icon="ph:lightbulb" class="insight-icon" />
            <span>作品最多发布于「{{ bestCategory }}」分类</span>
          </div>
          <div v-if="!pieChartOption" class="chart-empty">
            <Icon icon="ph:chart-pie" class="empty-icon" width="36" height="36" />
            <span>发布作品后，分类分布会在这里呈现</span>
          </div>
        </div>

        <!-- 作品排行 -->
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">作品排行</h3>
            <div class="granularity-tabs">
              <button class="tab-btn" :class="{ active: rankType === 'hot' }" @click="rankType = 'hot'">热度</button>
              <button class="tab-btn" :class="{ active: rankType === 'views' }" @click="rankType = 'views'">浏览</button>
              <button class="tab-btn" :class="{ active: rankType === 'likes' }" @click="rankType = 'likes'">点赞</button>
              <button class="tab-btn" :class="{ active: rankType === 'collections' }" @click="rankType = 'collections'">收藏</button>
            </div>
          </div>
          <v-chart v-if="barChartOption" :option="barChartOption" :autoresize="true" class="chart" @click="onBarClick" />
          <div class="chart-insight click-hint" v-if="barChartOption">
            <Icon icon="ph:hand-pointing" class="insight-icon" />
            <span>点击柱子可查看作品详情</span>
          </div>
          <div v-if="!barChartOption" class="chart-empty">
            <Icon icon="ph:barricade" class="empty-icon" width="36" height="36" />
            <span>发布作品后，排行数据会在这里呈现</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 互动转化 -->
    <div class="section-block">
      <div class="chart-card">
        <h3 class="chart-title" style="margin-bottom: 14px">互动转化</h3>
        <div class="conversion-grid">
          <div class="conversion-item">
            <div class="conversion-ring">
              <svg viewBox="0 0 100 100" class="ring-svg">
                <circle cx="50" cy="50" r="42" fill="none" stroke="var(--border, #e5e7eb)" stroke-width="8" />
                <circle cx="50" cy="50" r="42" fill="none" stroke="#ec4899" stroke-width="8"
                  stroke-dasharray="264" :stroke-dashoffset="likeRateDash" stroke-linecap="round"
                  transform="rotate(-90 50 50)" style="transition: stroke-dashoffset 0.6s" />
              </svg>
              <span class="ring-value">{{ likeRate }}%</span>
            </div>
            <span class="conversion-label">浏览→点赞率</span>
          </div>
          <div class="conversion-item">
            <div class="conversion-ring">
              <svg viewBox="0 0 100 100" class="ring-svg">
                <circle cx="50" cy="50" r="42" fill="none" stroke="var(--border, #e5e7eb)" stroke-width="8" />
                <circle cx="50" cy="50" r="42" fill="none" stroke="#a855f7" stroke-width="8"
                  stroke-dasharray="264" :stroke-dashoffset="collectRateDash" stroke-linecap="round"
                  transform="rotate(-90 50 50)" style="transition: stroke-dashoffset 0.6s" />
              </svg>
              <span class="ring-value">{{ collectRate }}%</span>
            </div>
            <span class="conversion-label">浏览→收藏率</span>
          </div>
          <div class="conversion-summary">
            <p class="summary-text" v-if="conversionInsight">{{ conversionInsight }}</p>
            <p class="summary-text" v-else>发布作品后将展示互动转化数据</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 口径提示 -->
    <div class="caliber-hint">
      <Icon icon="ph:info" class="hint-icon" />
      <span>数据基于已发布作品统计，趋势按选定周期聚合，与首页全量快照口径略有差异</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { DashboardVO } from '@/api/post'
import { getDashboard } from '@/api/post'

use([LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])

const emit = defineEmits<{
  goPost: [postId: string]
}>()

const dashboard = ref<DashboardVO>()
const granularity = ref<'daily' | 'monthly'>('daily')
const rankType = ref<'hot' | 'views' | 'likes' | 'collections'>('hot')

const pinkColor = '#c084fc'
const chartColors = [pinkColor, '#f0abfc', '#e9d5ff', '#f5d0fe', '#fbcfe8', '#d8b4fe', '#c4b5fd', '#a78bfa']

onMounted(async () => {
  try {
    const res = await getDashboard()
    dashboard.value = res
  } catch (e) {
    console.error('加载数据中心失败:', e)
  }
})

// ---- 趋势图 ----
const trendChartOption = computed(() => {
  const isDaily = granularity.value === 'daily'
  const trends = isDaily ? dashboard.value?.dailyTrends : dashboard.value?.monthlyTrends
  if (!trends?.length) return null
  const labels = trends.map((t: any) => isDaily ? t.date : t.month)
  const views = trends.map((t: any) => Number(t.views || 0))
  const likes = trends.map((t: any) => Number(t.likes || 0))
  const posts = trends.map((t: any) => Number(t.posts || 0))

  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['浏览', '点赞', '作品'],
      bottom: 0,
      textStyle: { color: '#909399', fontSize: 12 },
    },
    grid: { left: 50, right: 30, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#909399', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { color: '#909399', fontSize: 11 },
    },
    series: [
      {
        name: '浏览',
        type: 'line',
        data: views,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: pinkColor, width: 2 },
        itemStyle: { color: pinkColor },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(192,132,252,0.25)' },
              { offset: 1, color: 'rgba(192,132,252,0.02)' },
            ],
          },
        },
      },
      {
        name: '点赞',
        type: 'line',
        data: likes,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#f0abfc', width: 2 },
        itemStyle: { color: '#f0abfc' },
      },
      {
        name: '作品',
        type: 'line',
        data: posts,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#e9d5ff', width: 2, type: 'dashed' },
        itemStyle: { color: '#e9d5ff' },
      },
    ],
  }
})

// ---- 趋势洞察 ----
const trendInsight = computed(() => {
  const trends = granularity.value === 'daily'
    ? dashboard.value?.dailyTrends
    : dashboard.value?.monthlyTrends
  if (!trends?.length || trends.length < 2) return ''
  const label = granularity.value === 'daily' ? '天' : '月'
  const viewsList = trends.map((t: any) => t.views as number)
  const maxIdx = viewsList.reduce((i, v, j, a) => v > a[i] ? j : i, 0)
  const maxLabel = granularity.value === 'daily' ? (trends[maxIdx] as any).date : (trends[maxIdx] as any).month
  const first = viewsList[0] || 0
  const last = viewsList[viewsList.length - 1] || 0
  const change = first > 0 ? Math.round(((last - first) / first) * 100) : 0
  const dir = change >= 0 ? '上升' : '下降'
  const parts: string[] = []
  if (maxLabel) parts.push(`浏览量峰值在 ${maxLabel}`)
  if (change !== 0) parts.push(`整体${dir} ${Math.abs(change)}%`)
  return parts.join('，')
})

// ---- 分类洞察 ----
const bestCategory = computed(() => {
  const dist = dashboard.value?.categoryDistribution
  if (!dist?.length) return ''
  const best = dist.reduce((a, b) => a.count > b.count ? a : b)
  return best.name
})

// ---- 综合热度排行 ----
/** 热度 = 浏览量×1 + 点赞×3 + 收藏×5 */
const hotScore = (p: any) => (p.views ?? 0) * 1 + (p.likes ?? 0) * 3 + (p.collections ?? 0) * 5

const rankLabels: Record<string, string> = { hot: '热度', views: '浏览', likes: '点赞', collections: '收藏' }

const barChartOption = computed(() => {
  const posts = dashboard.value?.topPosts
  if (!posts?.length) return null
  let sorted: any[]
  if (rankType.value === 'hot') {
    sorted = [...posts].sort((a, b) => hotScore(b) - hotScore(a))
  } else {
    sorted = [...posts].sort((a, b) => {
      const va = (a as any)[rankType.value] ?? 0
      const vb = (b as any)[rankType.value] ?? 0
      return vb - va
    })
  }
  const names = sorted.map(p => (p.title || '').length > 8 ? (p.title || '').slice(0, 8) + '…' : (p.title || ''))
  const label = rankLabels[rankType.value] || '热度'
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const p = params[0]
        const post = sorted[p.dataIndex]
        if (!post) return `${p.name}<br/>${label}: ${p.value}`
        return `${p.name}<br/>${label}: ${p.value}<br/>浏览: ${post.views} | 点赞: ${post.likes} | 收藏: ${post.collections} | 评论: ${post.comments}`
      },
    },
    grid: { left: 40, right: 20, top: 10, bottom: 60 },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { color: '#909399', fontSize: 11, rotate: 20 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
    },
    yAxis: {
      type: 'value',
      name: label,
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { color: '#909399', fontSize: 11 },
    },
    series: [{
      type: 'bar',
      data: sorted.map((p, i) => ({
        value: rankType.value === 'hot' ? hotScore(p) : ((p as any)[rankType.value] ?? 0),
        itemStyle: { color: chartColors[i % chartColors.length], borderRadius: [4, 4, 0, 0] },
      })),
      barMaxWidth: 48,
    }],
  }
})

// ---- 分类分布饼图 ----
const pieChartOption = computed(() => {
  const dist = dashboard.value?.categoryDistribution
  if (!dist?.length) return null
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 篇 ({d}%)' },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { color: '#909399', fontSize: 11 },
    },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: dist.map((d, i) => ({ name: d.name, value: d.count, itemStyle: { color: chartColors[i % chartColors.length] } })),
    }],
  }
})

// ---- 互动转化率 ----
const totalViews = computed(() => dashboard.value?.summary?.totalViews ?? 0)
const totalLikes = computed(() => dashboard.value?.summary?.totalLikes ?? 0)
const totalCollections = computed(() => dashboard.value?.summary?.totalCollections ?? 0)

const likeRate = computed(() => {
  if (totalViews.value === 0) return 0
  return Math.round((totalLikes.value / totalViews.value) * 1000) / 10
})

const collectRate = computed(() => {
  if (totalViews.value === 0) return 0
  return Math.round((totalCollections.value / totalViews.value) * 1000) / 10
})

const ringPerimeter = 264 // 2 * PI * 42 ≈ 264
const likeRateDash = computed(() => ringPerimeter * (1 - likeRate.value / 100))
const collectRateDash = computed(() => ringPerimeter * (1 - collectRate.value / 100))

const conversionInsight = computed(() => {
  if (totalViews.value === 0) return ''
  const parts: string[] = []
  if (likeRate.value < 1) parts.push('点赞转化偏低，可优化内容吸引力')
  else if (likeRate.value > 5) parts.push('点赞转化率较高，继续保持')
  if (collectRate.value < 0.5) parts.push('收藏率偏低，可增加实用内容')
  else if (collectRate.value > 3) parts.push('收藏率表现优秀')
  return parts.join('；') || '转化数据正常'
})

// ---- 点击跳转 ----
function onBarClick(params: any) {
  const posts = dashboard.value?.topPosts
  if (!posts) return
  const sorted = rankType.value === 'hot'
    ? [...posts].sort((a, b) => hotScore(b) - hotScore(a))
    : [...posts].sort((a, b) => ((b as any)[rankType.value] ?? 0) - ((a as any)[rankType.value] ?? 0))
  if (sorted[params.dataIndex]) {
    emit('goPost', sorted[params.dataIndex].id)
  }
}
</script>

<style scoped>
.creator-data {
  padding: 28px 32px;
  max-width: 1100px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  margin: 0 0 6px;
}

.page-desc {
  font-size: 13px;
  color: var(--text-dim, #909399);
  margin: 0 0 28px;
}

.section-block {
  margin-bottom: 20px;
}

.chart-card {
  background: var(--card, #fff);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid var(--border, #f3f4f6);
  margin-bottom: 16px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  margin: 0;
}

.granularity-tabs {
  display: flex;
  gap: 4px;
  background: var(--bg-secondary, #f3f4f6);
  border-radius: 8px;
  padding: 3px;
}

.tab-btn {
  border: none;
  background: transparent;
  padding: 4px 14px;
  font-size: 12px;
  color: #909399;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: var(--card, #fff);
  color: #c084fc;
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}

.chart {
  width: 100%;
  height: 280px;
}

.chart-trend {
  height: 320px;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.charts-row .chart-card {
  margin-bottom: 0;
}

.chart-empty {
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #c0c4cc;
  font-size: 13px;
}

.empty-icon {
  color: #c084fc;
  opacity: 0.45;
}

/* 洞察结论 */
.chart-insight {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin-top: 10px;
  background: var(--bg-secondary, #faf5ff);
  border-radius: 8px;
  font-size: 12px;
  color: var(--text-dim, #909399);
}

.chart-insight.click-hint {
  background: transparent;
  padding: 6px 0 0;
  margin-top: 4px;
  color: #c0c4cc;
}

.insight-icon {
  width: 14px;
  height: 14px;
  color: #a78bfa;
  flex-shrink: 0;
}

/* 互动转化 */
.conversion-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 2fr;
  gap: 16px;
  align-items: center;
}

.conversion-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.conversion-ring {
  position: relative;
  width: 90px;
  height: 90px;
}

.ring-svg {
  width: 100%;
  height: 100%;
}

.ring-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
}

.conversion-label {
  font-size: 12px;
  color: var(--text-dim, #909399);
}

.conversion-summary {
  padding-left: 8px;
  border-left: 1px solid var(--border, #e5e7eb);
}

.summary-text {
  font-size: 13px;
  color: var(--text-dim, #909399);
  line-height: 1.6;
  margin: 0;
}

/* 口径提示 */
.caliber-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--bg-secondary, #faf5ff);
  border-radius: 10px;
  font-size: 12px;
  color: var(--text-dim, #909399);
  margin-top: 8px;
}

.hint-icon {
  width: 16px;
  height: 16px;
  color: #a78bfa;
  flex-shrink: 0;
}
</style>
