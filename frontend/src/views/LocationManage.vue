<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">楼栋管理</h2>
        <p class="page-desc">维护广州大学各校区的宿舍楼栋信息。学生报修时选择楼栋并自行填写房间号。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openBuilding">新增楼栋</el-button>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <span class="card-title">楼栋列表</span>
          <el-tag effect="plain" round>{{ buildings.length }} 栋</el-tag>
        </div>
      </template>

      <el-table :data="buildings" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="楼栋名称" min-width="150">
          <template #default="{ row }">
            <span class="building-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="campus" label="所属校区" width="140">
          <template #default="{ row }">
            <el-tag effect="plain" round>{{ row.campus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165" />
        <template #empty>
          <div class="empty-tip">暂无楼栋，点击右上角新增</div>
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="buildingVisible" title="新增楼栋" width="460px">
      <el-form ref="buildingRef" :model="buildingForm" :rules="buildingRules" label-position="top">
        <el-form-item label="楼栋名称" prop="name">
          <el-input v-model="buildingForm.name" placeholder="例如：梅苑3栋" />
        </el-form-item>
        <el-form-item label="所属校区" prop="campus">
          <el-select v-model="buildingForm.campus" style="width: 100%">
            <el-option label="大学城校区" value="大学城校区" />
            <el-option label="桂花岗校区" value="桂花岗校区" />
            <el-option label="黄埔研究院" value="黄埔研究院" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="buildingForm.address" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="buildingForm.remark" placeholder="例如：男生宿舍" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="buildingVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBuilding">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getLocations, addBuilding } from '../api/location'

const buildings = ref([])
const submitting = ref(false)
const buildingVisible = ref(false)
const buildingRef = ref()

const buildingForm = reactive({ name: '', campus: '大学城校区', address: '', remark: '' })

const buildingRules = {
  name: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }],
  campus: [{ required: true, message: '请选择校区', trigger: 'change' }]
}

async function loadLocations() {
  try {
    const data = await getLocations()
    buildings.value = data || []
  } catch (e) {
    buildings.value = []
  }
}

function openBuilding() {
  Object.assign(buildingForm, { name: '', campus: '大学城校区', address: '', remark: '' })
  buildingVisible.value = true
}

async function submitBuilding() {
  if (!buildingRef.value) return
  await buildingRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await addBuilding({ ...buildingForm })
      ElMessage.success('楼栋创建成功')
      buildingVisible.value = false
      await loadLocations()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(loadLocations)
</script>

<style scoped>
.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #1b2437;
}

.building-name {
  font-weight: 600;
  color: #1f5fd6;
}
</style>
