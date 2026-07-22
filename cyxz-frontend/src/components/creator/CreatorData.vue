<template>
  <div class="creator-data">
    <h2 class="page-title">数据中心</h2>

    <!-- 趋势图 -->
    <div class="chart-card">
      <div class="chart-header">
        <h3 class="chart-title">数据趋势</h3>
        <div class="granularity-tabs">
          <button
            class="tab-btn"
            :class="{ active: granularity === 'daily' }"
            @click="granularity = 'daily'"
          >近30天</button>
          <button
            class="tab-btn"
            :class="{ active: granularity === 'monthly' }"
            @click="granularity = 'monthly'"
          >按月</button>
        </div>
      </div>
      <v-chart v-if="trendChartOption" :option="trendChartOption" :autoresize="true" class="chart chart-trend" />
      <div v-else class="chart-empty">
        <Icon icon="ph:chart-line" class="empty-icon" width="36" height="36" />
        <span>发布作品后，趋势数据会在这里呈现</span>
      </div>
    </div>

    <!-- 下排：分类分布 + 作品排行 -->
    <div class="charts-row">
      <div class="chart-card">
        <h3 class="chart-title">分类分布</h3>
        <v-chart v-if="pieChartOption" :option="pieChartOption" :autoresize="true" class="chart" />
        <div v-else class="chart-empty">
          <Icon icon="ph:chart-pie" class="empty-icon" width="36" height="36" />
          <span>发布作品后，分类分布会在这里呈现</span>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="chart-title">作品浏览排行 Top 5</h3>
        <v-chart v-if="barChartOption" :option="barChartOption" :autoresize="true" class="chart" />
        <div v-else class="chart-empty">
          <Icon icon="ph:barricade" class="empty-icon" width="36" height="36" />
          <span>发布作品后，排行数据会在这里呈现</span>
        </div>
      </div>
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

const dashboard = ref<DashboardVO>()
const granularity = ref<'daily' | 'monthly'>('daily')

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

/** 趋势图：根据 granularity 切换日/月数据 */
const trendChartOption = computed(() => {
  const isDaily = granularity.value === 'daily'
  const trends = isDaily ? dashboard.value?.dailyTrends : dashboard.value?.monthlyTrends
  if (!trends?.length) return null
  const labels = trends.map((t: any) => isDaily ? t.date : t.month)
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
        data: trends.map((t: any) => t.views),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: pinkColor, width: 2 },
        itemStyle: { color: pinkColor },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(192,132,252,0.25)' }, { offset: 1, color: 'rgba(192,132,252,0.02)' }] } },
      },
      {
        name: '点赞',
        type: 'line',
        data: trends.map((t: any) => t.likes),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#f0abfc', width: 2 },
        itemStyle: { color: '#f0abfc' },
      },
      {
        name: '作品',
        type: 'line',
        data: trends.map((t: any) => t.posts),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#e9d5ff', width: 2, type: 'dashed' },
        itemStyle: { color: '#e9d5ff' },
      },
    ],
  }
})

/** 分类分布饼图 */
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

/** 浏览排行柱状图 */
const barChartOption = computed(() => {
  const posts = dashboard.value?.topPosts
  if (!posts?.length) return null
  const names = posts.map(p => p.title.length > 8 ? p.title.slice(0, 8) + '…' : p.title)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 20, top: 10, bottom: 60 },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { color: '#909399', fontSize: 11, rotate: 20 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
    },
    yAxis: {
      type: 'value',
      name: '浏览',
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { color: '#909399', fontSize: 11 },
    },
    series: [{
      type: 'bar',
      data: posts.map((p, i) => ({ value: p.views, itemStyle: { color: chartColors[i % chartColors.length], borderRadius: [4, 4, 0, 0] } })),
      barMaxWidth: 48,
    }],
  }
})
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
  margin: 0 0 24px;
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #f3f4f6;
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
  background: #f3f4f6;
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
  background: #fff;
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
</style>
