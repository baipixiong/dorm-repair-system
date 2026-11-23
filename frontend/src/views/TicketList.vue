<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ pageTitle }}</h2>
        <p class="page-desc">{{ pageDesc }}</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="loadTickets">刷新</el-button>
        <el-button v-if="userStore.isAdmin" :icon="Download" :loading="exporting" @click="handleExport">
          导出 CSV
        </el-button>
        <el-button v-if="userStore.isStudent" type="primary" :icon="Plus" @click="$router.push('/tickets/create')">
          我要报修
        </el-button>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="filters.keyword"
            placeholder="工单编号 / 标题 / 描述"
            clearable
            style="width: 220px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="filters.category" placeholder="全部类别" clearable style="width: 140px">
            <el-option v-for="item in CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select v-model="filters.buildingId" placeholder="全部楼栋" clearable style="width: 140px">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="提交日期">
          <el-date-picker
            v-model="filters.dates"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" style="width: 100%" @row-click="(row) => openDetail(row.id)">
        <el-table-column prop="ticketNo" label="工单编号" width="160">
          <template #default="{ row }">
            <span class="ticket-no">{{ row.ticketNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="报修标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="title-cell">{{ row.title }}</div>
            <div class="sub-cell">{{ row.description }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类别" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" round>{{ row.categoryLabel || categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报修位置" width="150">
          <template #default="{ row }">{{ locationText(row) }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" effect="light" size="small" round>
              {{ row.priorityLabel || priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light" round>
              {{ row.statusLabel || statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报修人" width="100">
          <template #default="{ row }">{{ row.studentName || '-' }}</template>
        </el-table-column>
        <el-table-column label="维修人员" width="110">
          <template #default="{ row }">
            <span v-if="row.assigneeName">{{ row.assigneeName }}</span>
            <span v-else class="text-muted">未派单</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="165" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click.stop="openDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-tip">暂无符合条件的工单</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :page-sizes="[10, 20, 50]"
          :total="page.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadTickets"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Download, Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import { getTickets, exportTickets } from '../api/ticket'
import { getLocations } from '../api/location'
import { userStore } from '../store/user'
import {
  STATUS_OPTIONS,
  CATEGORY_OPTIONS,
  locationText,
  statusLabel,
  statusType,
  categoryLabel,
  priorityLabel,
  priorityType
} from '../utils/constants'

const router = useRouter()
const loading = ref(false)
const exporting = ref(false)
const list = ref([])
const buildings = ref([])

const filters = reactive({
  keyword: '',
  status: '',
  category: '',
  buildingId: null,
  dates: []
})

const page = reactive({
  current: 1,
  size: 10,
  total: 0
})

const pageTitle = computed(() => {
  if (userStore.isAdmin) return '工单管理'
  if (userStore.isTechnician) return '我的维修工单'
  return '我的报修'
})

const pageDesc = computed(() => {
  if (userStore.isAdmin) return '查看全校宿舍报修工单，确认分类、派单并跟踪处理进度。'
  if (userStore.isTechnician) return '查看分配给自己的维修工单，按预约时间上门处理。'
  return '查看自己提交的报修工单及最新处理进度。'
})

function buildParams() {
  return {
    keyword: filters.keyword || undefined,
    status: filters.status || undefined,
    category: filters.category || undefined,
    buildingId: filters.buildingId || undefined,
    start: filters.dates?.[0] || undefined,
    end: filters.dates?.[1] || undefined
  }
}

async function loadTickets() {
  loading.value = true
  try {
    const data = await getTickets({
      ...buildParams(),
      page: page.current,
      size: page.size
    })
    list.value = data?.list || []
    page.total = data?.total || 0
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function search() {
  page.current = 1
  loadTickets()
}

function reset() {
  filters.keyword = ''
  filters.status = ''
  filters.category = ''
  filters.buildingId = null
  filters.dates = []
  page.current = 1
  loadTickets()
}

function handleSizeChange() {
  page.current = 1
  loadTickets()
}

async function loadLocations() {
  try {
    buildings.value = await getLocations()
  } catch (e) {
    buildings.value = []
  }
}

async function handleExport() {
  exporting.value = true
  try {
    await exportTickets(buildParams())
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function openDetail(id) {
  router.push(`/tickets/${id}`)
}

onMounted(() => {
  loadLocations()
  loadTickets()
})
</script>

<style scoped>
.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ticket-no {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 12.5px;
  color: #1f5fd6;
  font-weight: 600;
}

.title-cell {
  font-weight: 600;
  color: #1b2437;
  font-size: 13.5px;
}

.sub-cell {
  font-size: 11.5px;
  color: #a0acbf;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px;
}

:deep(.el-table__row) {
  cursor: pointer;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
