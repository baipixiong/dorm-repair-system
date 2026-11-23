<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">楼栋管理</h2>
        <p class="page-desc">维护广州大学各校区的宿舍楼栋与房间信息，供学生报修时选择。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openBuilding">新增楼栋</el-button>
    </div>

    <div class="location-grid">
      <el-card shadow="never" class="building-card">
        <template #header>
          <div class="flex-between">
            <span class="card-title">楼栋列表</span>
            <el-tag effect="plain" round>{{ buildings.length }} 栋</el-tag>
          </div>
        </template>
        <div v-if="buildings.length === 0" class="empty-tip">暂无楼栋</div>
        <div v-else class="building-list">
          <div
            v-for="building in buildings"
            :key="building.id"
            class="building-item"
            :class="{ active: building.id === selectedId }"
            @click="selectedId = building.id"
          >
            <div class="building-icon">
              <el-icon><OfficeBuilding /></el-icon>
            </div>
            <div class="building-info">
              <div class="building-name">{{ building.name }}</div>
              <div class="building-sub">{{ building.campus }} · {{ building.roomCount || 0 }} 个房间</div>
            </div>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="flex-between">
            <div>
              <span class="card-title">{{ selected ? selected.name : '房间列表' }}</span>
              <span v-if="selected" class="room-sub">{{ selected.campus }} · {{ selected.address || '暂无地址' }}</span>
            </div>
            <el-button type="primary" plain :icon="Plus" :disabled="!selected" @click="openRoom">新增房间</el-button>
          </div>
        </template>

        <el-table :data="rooms" style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="roomNo" label="房间号" width="120">
            <template #default="{ row }"><span class="room-no">{{ row.roomNo }}</span></template>
          </el-table-column>
          <el-table-column prop="floor" label="楼层" width="90">
            <template #default="{ row }">{{ row.floor ? `${row.floor} 层` : '-' }}</template>
          </el-table-column>
          <el-table-column prop="roomType" label="房型" width="110" />
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" width="165" />
          <template #empty>
            <div class="empty-tip">该楼栋暂无房间，点击右上角新增</div>
          </template>
        </el-table>
      </el-card>
    </div>

    <el-dialog v-model="buildingVisible" title="新增楼栋" width="460px">
      <el-form ref="buildingRef" :model="buildingForm" :rules="buildingRules" label-position="top">
        <el-form-item label="楼栋名称" prop="name">
          <el-input v-model="buildingForm.name" placeholder="例如：梅苑3栋" />
        </el-form-item>
        <el-form-item label="所属校区" prop="campus">
          <el-select v-model="buildingForm.campus" style="width: 100%">
            <el-option label="大学城校区" value="大学城校区" />
            <el-option label="桂花岗校区" value="桂花岗校区" />
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

    <el-dialog v-model="roomVisible" title="新增房间" width="460px">
      <el-form ref="roomRef" :model="roomForm" :rules="roomRules" label-position="top">
        <el-form-item label="所属楼栋">
          <el-input :model-value="selected?.name" disabled />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="房间号" prop="roomNo">
            <el-input v-model="roomForm.roomNo" placeholder="例如：301" />
          </el-form-item>
          <el-form-item label="楼层">
            <el-input-number v-model="roomForm.floor" :min="1" :max="30" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="房型">
          <el-select v-model="roomForm.roomType" style="width: 100%">
            <el-option label="四人间" value="四人间" />
            <el-option label="六人间" value="六人间" />
            <el-option label="二人间" value="二人间" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="roomForm.remark" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRoom">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, OfficeBuilding, ArrowRight } from '@element-plus/icons-vue'
import { getLocations, addBuilding, addRoom } from '../api/location'

const buildings = ref([])
const selectedId = ref(null)
const submitting = ref(false)
const buildingVisible = ref(false)
const roomVisible = ref(false)
const buildingRef = ref()
const roomRef = ref()

const buildingForm = reactive({ name: '', campus: '大学城校区', address: '', remark: '' })
const roomForm = reactive({ roomNo: '', floor: 1, roomType: '四人间', remark: '' })

const selected = computed(() => buildings.value.find((item) => item.id === selectedId.value))
const rooms = computed(() => selected.value?.rooms || [])

const buildingRules = {
  name: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }],
  campus: [{ required: true, message: '请选择校区', trigger: 'change' }]
}

const roomRules = {
  roomNo: [{ required: true, message: '请输入房间号', trigger: 'blur' }]
}

async function loadLocations(keepId) {
  try {
    const data = await getLocations()
    buildings.value = data || []
    const exists = buildings.value.some((item) => item.id === keepId)
    if (exists) {
      selectedId.value = keepId
    } else if (!buildings.value.some((item) => item.id === selectedId.value)) {
      selectedId.value = buildings.value[0]?.id || null
    }
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
      const building = await addBuilding({ ...buildingForm })
      ElMessage.success('楼栋创建成功')
      buildingVisible.value = false
      await loadLocations(building.id)
    } finally {
      submitting.value = false
    }
  })
}

function openRoom() {
  Object.assign(roomForm, { roomNo: '', floor: 1, roomType: '四人间', remark: '' })
  roomVisible.value = true
}

async function submitRoom() {
  if (!roomRef.value) return
  await roomRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await addRoom({ ...roomForm, buildingId: selectedId.value })
      ElMessage.success('房间创建成功')
      roomVisible.value = false
      await loadLocations(selectedId.value)
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => loadLocations())
</script>

<style scoped>
.location-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #1b2437;
}

.room-sub {
  margin-left: 10px;
  font-size: 12px;
  color: #93a0b5;
}

.building-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 560px;
  overflow-y: auto;
}

.building-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #eef2f8;
  cursor: pointer;
  transition: all 0.18s;
}

.building-item:hover {
  border-color: #c0d3f6;
  background: #f8faff;
}

.building-item.active {
  border-color: #1f5fd6;
  background: #eef4ff;
  box-shadow: 0 6px 16px rgba(31, 95, 214, 0.12);
}

.building-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #eef3fd;
  color: #1f5fd6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.building-info {
  flex: 1;
  min-width: 0;
}

.building-name {
  font-size: 13.5px;
  font-weight: 600;
  color: #1b2437;
}

.building-sub {
  font-size: 11.5px;
  color: #93a0b5;
  margin-top: 2px;
}

.arrow {
  color: #b6c2d4;
}

.room-no {
  font-weight: 600;
  color: #1f5fd6;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 980px) {
  .location-grid {
    grid-template-columns: 1fr;
  }
}
</style>
