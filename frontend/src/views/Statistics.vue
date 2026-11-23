<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ userStore.isAdmin ? '数据统计' : '我的统计' }}</h2>
        <p class="page-desc">按楼栋、类别与时间维度查看报修工单分布与处理效率。</p>
      </div>
      <el-button :icon="Refresh" @click="loadData">刷新数据</el-button>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="楼栋">
          <el-select v-model="filters.buildingId" placeholder="全部楼栋" clearable style="width: 160px">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="统计时间">
          <el-date-picker
            v-model="filters.dates"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">工单总数</div>
        <div class="stat-value">{{ stats.total ?? 0 }}</div>
        <div class="stat-extra">今日新增 {{ stats.todayNew ?? 0 }} 单</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">未完成工单</div>
        <div class="stat-value" style="color: #d97706">{{ stats.unfinished ?? 0 }}</div>
        <div class="stat-extra">待派单 {{ stats.pending ?? 0 }} · 维修中 {{ stats.working ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">完成率</div>
        <div class="stat-value" style="color: #0f9f6e">{{ stats.completionRate ?? 0 }}<small>%</small></div>
        <div class="stat-extra">已完成 {{ stats.completed ?? 0 }} 单</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">平均维修时长</div>
        <div class="stat-value" style="color: #7c3aed">{{ stats.avgCompletionHours || 0 }}<small>h</small></div>
        <div class="stat-extra">从提交到验收的平均耗时</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">平均评价</div>
        <div class="stat-value" style="color: #eab308">{{ stats.avgScore || '—' }}</div>
        <div class="stat-extra">共 {{ stats.evaluationCount ?? 0 }} 条评价</div>
      </div>
    </div>

    <div class="chart-grid">
      <el-card class="chart-card wide" shadow="never">
        <template #header><span class="card-title">近 7 天工单趋势</span></template>
        <div ref="trendEl" class="chart"></div>
      </el-card>

      <el-card class="chart-card" shadow="never">
        <template #header><span class="card-title">工单状态分布</span></template>
        <div ref="statusEl" class="chart"></div>
      </el-card>

      <el-card class="chart-card" shadow="never">
        <template #header><span class="card-title">报修类别分布</span></template>
        <div ref="categoryEl" class="chart"></div>
      </el-card>

      <el-card class="chart-card wide" shadow="never">
        <template #header><span class="card-title">各楼栋工单数量</span></template>
        <div ref="buildingEl" class="chart"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { Refresh, Search, RefreshLeft } from '@element-plus/icons-vue'
import { getStatistics } from '../api/statistics'
import { getLocations } from '../api/location'
import { userStore } from '../store/user'
import { STATUS_OPTIONS } from '../utils/constants'

const stats = ref({})
const buildings = ref([])
const filters = reactive({ buildingId: null, dates: [] })

const trendEl = ref()
const statusEl = ref()
const categoryEl = ref()
const buildingEl = ref()
const charts = {}

const palette = ['#1f5fd6', '#12b3a8', '#f59e0b', '#7c3aed', '#0f9f6e', '#94a3b8']

async function loadData() {
  try {
    const data = await getStatistics({
      buildingId: filters.buildingId || undefined,
      start: filters.dates?.[0] || undefined,
      end: filters.dates?.[1] || undefined
    })
    stats.value = data || {}
    await nextTick()
    renderCharts()
  } catch (e) {
    // 错误已提示
  }
}

function initChart(el, key) {
  if (!el) return null
  if (!charts[key]) {
    charts[key] = echarts.init(el)
  }
  return charts[key]
}

function renderCharts() {
  renderTrend()
  renderStatus()
  renderCategory()
  renderBuilding()
}

function renderTrend() {
  const chart = initChart(trendEl.value, 'trend')
  if (!chart) return
  const trend = stats.value.trend || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0, right: 0, icon: 'roundRect', textStyle: { color: '#6b778c', fontSize: 12 } },
    grid: { left: 10, right: 16, top: 40, bottom: 6, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.map((item) => item.date),
      axisLine: { lineStyle: { color: '#e3e9f2' } },
      axisLabel: { color: '#8b98ad' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef2f8' } },
      axisLabel: { color: '#8b98ad' }
    },
    series: [
      {
        name: '新增工单',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: trend.map((item) => item.created),
        itemStyle: { color: '#1f5fd6' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(31,95,214,0.28)' },
            { offset: 1, color: 'rgba(31,95,214,0.02)' }
          ])
        }
      },
      {
        name: '完成工单',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: trend.map((item) => item.completed),
        itemStyle: { color: '#12b3a8' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(18,179,168,0.24)' },
            { offset: 1, color: 'rgba(18,179,168,0.02)' }
          ])
        }
      }
    ]
  }, true)
}

function renderStatus() {
  const chart = initChart(statusEl.value, 'status')
  if (!chart) return
  const data = STATUS_OPTIONS.map((item) => ({
    name: item.label,
    value: stats.value[item.value] || 0
  })).filter((item) => item.value > 0)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
    legend: { bottom: 0, icon: 'circle', textStyle: { color: '#6b778c', fontSize: 12 } },
    color: ['#94a3b8', '#f59e0b', '#d97706', '#7c3aed', '#0f9f6e', '#cbd5e1'],
    series: [
      {
        type: 'pie',
        radius: ['42%', '66%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        data: data.length ? data : [{ name: '暂无数据', value: 1, itemStyle: { color: '#eef2f8' } }]
      }
    ]
  }, true)
}

function renderCategory() {
  const chart = initChart(categoryEl.value, 'category')
  if (!chart) return
  const data = stats.value.categoryStats || []
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 18, top: 16, bottom: 6, containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.name),
      axisLine: { lineStyle: { color: '#e3e9f2' } },
      axisLabel: { color: '#8b98ad', interval: 0, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef2f8' } },
      axisLabel: { color: '#8b98ad' }
    },
    series: [
      {
        type: 'bar',
        barWidth: 26,
        data: data.map((item, index) => ({
          value: item.value,
          itemStyle: {
            borderRadius: [8, 8, 0, 0],
            color: palette[index % palette.length]
          }
        }))
      }
    ]
  }, true)
}

function renderBuilding() {
  const chart = initChart(buildingEl.value, 'building')
  if (!chart) return
  const data = stats.value.buildingStats || []
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 24, top: 16, bottom: 6, containLabel: true },
    xAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef2f8' } },
      axisLabel: { color: '#8b98ad' }
    },
    yAxis: {
      type: 'category',
      data: data.map((item) => item.name),
      axisLine: { lineStyle: { color: '#e3e9f2' } },
      axisLabel: { color: '#8b98ad' }
    },
    series: [
      {
        type: 'bar',
        barWidth: 18,
        data: data.map((item, index) => ({
          value: item.value,
          itemStyle: {
            borderRadius: [0, 8, 8, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: palette[index % palette.length] },
              { offset: 1, color: '#9cc3ff' }
            ])
          }
        }))
      }
    ]
  }, true)
}

async function loadLocations() {
  try {
    buildings.value = await getLocations()
  } catch (e) {
    buildings.value = []
  }
}

function reset() {
  filters.buildingId = null
  filters.dates = []
  loadData()
}

function onResize() {
  Object.values(charts).forEach((chart) => chart.resize())
}

onMounted(() => {
  loadLocations()
  loadData()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  Object.values(charts).forEach((chart) => chart.dispose())
})
</script>

<style scoped>
.stat-value small {
  font-size: 13px;
  font-weight: 500;
  margin-left: 3px;
  color: #93a0b5;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card.wide {
  grid-column: span 2;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #1b2437;
}

.chart {
  height: 300px;
}

@media (max-width: 900px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.wide {
    grid-column: span 1;
  }
}
</style>
