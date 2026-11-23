<template>
  <div class="page dashboard">
    <section class="hero">
      <div class="hero-orb"></div>
      <div class="hero-orb small"></div>
      <div class="hero-main">
        <div class="hero-greet">{{ greeting }}，{{ userStore.displayName }}</div>
        <h1>宿舍报修工作台</h1>
        <p>{{ heroDesc }}</p>
        <div class="hero-actions">
          <el-button v-if="userStore.isStudent" type="primary" size="large" class="hero-btn" @click="$router.push('/tickets/create')">
            <el-icon><EditPen /></el-icon>立即报修
          </el-button>
          <el-button size="large" class="hero-ghost" @click="$router.push('/tickets')">
            <el-icon><Tickets /></el-icon>{{ userStore.isStudent ? '我的报修' : '查看工单' }}
          </el-button>
          <el-button size="large" class="hero-ghost" @click="$router.push('/statistics')">
            <el-icon><DataAnalysis /></el-icon>数据统计
          </el-button>
        </div>
      </div>
      <div class="hero-side">
        <div class="hero-badge">
          <span class="badge-label">完成率</span>
          <span class="badge-value">{{ stats.completionRate ?? 0 }}<small>%</small></span>
        </div>
        <div class="hero-meta">
          <div><span>今日新增</span><strong>{{ stats.todayNew ?? 0 }}</strong></div>
          <div><span>平均评分</span><strong>{{ stats.avgScore || '—' }}</strong></div>
        </div>
      </div>
    </section>

    <div class="stat-grid">
      <div v-for="card in cards" :key="card.key" class="stat-card">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value" :style="{ color: card.color }">{{ stats[card.key] ?? 0 }}</div>
        <div class="stat-extra">{{ card.extra }}</div>
        <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
      </div>
    </div>

    <div class="dash-grid">
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="flex-between">
            <div>
              <div class="card-title">报修类别分布</div>
              <div class="card-sub">按工单类别统计</div>
            </div>
            <el-button text type="primary" @click="$router.push('/statistics')">查看详情</el-button>
          </div>
        </template>
        <div ref="chartEl" class="chart"></div>
      </el-card>

      <el-card class="recent-card" shadow="never">
        <template #header>
          <div class="flex-between">
            <div>
              <div class="card-title">最新工单</div>
              <div class="card-sub">最近提交的报修记录</div>
            </div>
            <el-button text type="primary" @click="$router.push('/tickets')">全部工单</el-button>
          </div>
        </template>

        <div v-if="tickets.length === 0" class="empty-tip">暂无工单数据</div>
        <div v-else class="recent-list">
          <div v-for="ticket in tickets" :key="ticket.id" class="recent-item" @click="openTicket(ticket.id)">
            <div class="recent-left">
              <span class="recent-dot" :class="ticket.status"></span>
              <div>
                <div class="recent-title">{{ ticket.title }}</div>
                <div class="recent-meta">
                  {{ ticket.ticketNo }} · {{ locationText(ticket) }}
                </div>
              </div>
            </div>
            <div class="recent-right">
              <el-tag :type="statusType(ticket.status)" effect="light" size="small" round>
                {{ statusLabel(ticket.status) }}
              </el-tag>
              <span class="recent-time">{{ ticket.createdAt }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { EditPen, Tickets, DataAnalysis, Clock, Tools, CircleCheck, Finished } from '@element-plus/icons-vue'
import { getStatistics } from '../api/statistics'
import { getTickets } from '../api/ticket'
import { userStore } from '../store/user'
import { locationText, statusLabel, statusType } from '../utils/constants'

const router = useRouter()
const stats = ref({})
const tickets = ref([])
const chartEl = ref()
let chart = null

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const heroDesc = computed(() => {
  if (userStore.isAdmin) return '这里汇总了全校宿舍报修工单，请及时确认分类并安排维修人员上门处理。'
  if (userStore.isTechnician) return '这里是你负责的维修工单，请按预约时间上门处理并及时反馈维修进度。'
  return '宿舍设施出现问题？在这里一键提交报修，随时查看维修进度与验收结果。'
})

const cards = computed(() => [
  { key: 'pending', label: '待派单', color: '#1f5fd6', bg: '#eaf1fd', icon: Clock, extra: '等待管理员安排维修人员' },
  { key: 'working', label: '维修中', color: '#d97706', bg: '#fef3e2', icon: Tools, extra: '维修人员正在处理' },
  { key: 'review', label: '待验收', color: '#7c3aed', bg: '#f1ebfe', icon: CircleCheck, extra: '等待学生确认维修结果' },
  { key: 'completed', label: '已完成', color: '#0f9f6e', bg: '#e6f7f0', icon: Finished, extra: '已验收完成的工单' }
])

async function loadData() {
  try {
    const [statData, ticketPage] = await Promise.all([
      getStatistics(),
      getTickets({ page: 1, size: 6 })
    ])
    stats.value = statData || {}
    tickets.value = ticketPage?.list || []
    await nextTick()
    renderChart()
  } catch (e) {
    // 错误已由拦截器提示
  }
}

function renderChart() {
  if (!chartEl.value) return
  if (!chart) {
    chart = echarts.init(chartEl.value)
  }
  const data = (stats.value.categoryStats || []).map((item) => ({
    name: item.name,
    value: item.value
  }))
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
    legend: { bottom: 0, icon: 'circle', textStyle: { color: '#6b778c', fontSize: 12 } },
    color: ['#1f5fd6', '#12b3a8', '#f59e0b', '#7c3aed', '#94a3b8'],
    series: [
      {
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        data
      }
    ]
  })
}

function openTicket(id) {
  router.push(`/tickets/${id}`)
}

function onResize() {
  chart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.hero {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  padding: 30px 32px;
  background: linear-gradient(120deg, #122c5c 0%, #1f5fd6 58%, #12b3a8 130%);
  color: #fff;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
  box-shadow: 0 18px 40px rgba(20, 60, 130, 0.25);
}

.hero-orb {
  position: absolute;
  width: 300px;
  height: 300px;
  right: -60px;
  top: -140px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.hero-orb.small {
  width: 160px;
  height: 160px;
  right: 180px;
  top: auto;
  bottom: -100px;
  background: rgba(255, 255, 255, 0.06);
}

.hero-main {
  position: relative;
  z-index: 2;
}

.hero-greet {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 8px;
}

.hero h1 {
  margin: 0 0 10px;
  font-size: 27px;
  font-weight: 700;
  letter-spacing: 1px;
}

.hero p {
  margin: 0 0 20px;
  max-width: 620px;
  font-size: 13px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.78);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-btn {
  background: #fff;
  color: #1f5fd6;
  border: none;
  font-weight: 600;
}

.hero-btn:hover {
  background: #eef4ff;
  color: #1f5fd6;
}

.hero-ghost {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.28);
  color: #fff;
}

.hero-ghost:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.hero-side {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 14px;
  min-width: 190px;
}

.hero-badge {
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 16px;
  padding: 14px 20px;
  text-align: right;
  backdrop-filter: blur(4px);
}

.badge-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 4px;
}

.badge-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}

.badge-value small {
  font-size: 14px;
  font-weight: 500;
  margin-left: 2px;
}

.hero-meta {
  display: flex;
  gap: 22px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  text-align: right;
}

.hero-meta strong {
  display: block;
  font-size: 18px;
  color: #fff;
  margin-top: 3px;
}

.dash-grid {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #1b2437;
}

.card-sub {
  font-size: 12px;
  color: #93a0b5;
  margin-top: 3px;
}

.chart {
  height: 290px;
}

.recent-list {
  display: flex;
  flex-direction: column;
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 13px 4px;
  border-bottom: 1px dashed #eef2f8;
  cursor: pointer;
  transition: background 0.15s;
}

.recent-item:last-child {
  border-bottom: none;
}

.recent-item:hover {
  background: #f8faff;
}

.recent-left {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  min-width: 0;
}

.recent-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  background: #94a3b8;
  flex-shrink: 0;
}

.recent-dot.pending { background: #1f5fd6; }
.recent-dot.assigned { background: #f59e0b; }
.recent-dot.working { background: #d97706; }
.recent-dot.review { background: #7c3aed; }
.recent-dot.completed { background: #0f9f6e; }
.recent-dot.cancelled { background: #94a3b8; }

.recent-title {
  font-size: 13.5px;
  font-weight: 600;
  color: #1b2437;
  margin-bottom: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-meta {
  font-size: 11.5px;
  color: #93a0b5;
}

.recent-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
  flex-shrink: 0;
}

.recent-time {
  font-size: 11px;
  color: #a4b0c2;
}

@media (max-width: 1080px) {
  .dash-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero {
    flex-direction: column;
    padding: 24px 22px;
  }

  .hero-side {
    align-items: flex-start;
    flex-direction: row;
    justify-content: flex-start;
  }

  .hero-badge {
    text-align: left;
  }

  .hero h1 {
    font-size: 22px;
  }
}
</style>
