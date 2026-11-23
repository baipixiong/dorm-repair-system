<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">操作日志</h2>
        <p class="page-desc">记录登录、报修、派单、验收、用户与楼栋管理等关键操作，最近 100 条。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadLogs">刷新</el-button>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="操作人 / 操作类型 / 详情" clearable style="width: 280px" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="filtered" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="actorName" label="操作人" width="130" />
        <el-table-column label="操作类型" width="140">
          <template #default="{ row }">
            <el-tag effect="plain" round>{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="操作详情" min-width="320" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="操作时间" width="180" />
        <template #empty>
          <div class="empty-tip">暂无操作日志</div>
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getAuditLogs } from '../api/user'

const loading = ref(false)
const keyword = ref('')
const logs = ref([])

const filtered = computed(() => {
  const key = keyword.value.trim()
  if (!key) return logs.value
  return logs.value.filter((item) =>
    [item.actorName, item.action, item.detail].some((value) => (value || '').includes(key))
  )
})

async function loadLogs() {
  loading.value = true
  try {
    logs.value = await getAuditLogs()
  } catch (e) {
    logs.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>
