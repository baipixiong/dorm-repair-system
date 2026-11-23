<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">管理系统中的学生、维修人员与管理员账号，可启用或停用账号。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="filters.keyword"
            placeholder="账号 / 姓名 / 学号 / 手机号"
            clearable
            style="width: 240px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.role" placeholder="全部角色" clearable style="width: 140px">
            <el-option v-for="item in ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号" width="130">
          <template #default="{ row }">
            <span class="username">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleType(row.role)" effect="light" round>{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号/工号" width="130" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="所属宿舍" min-width="140">
          <template #default="{ row }">
            <span v-if="row.buildingName">{{ row.buildingName }} {{ row.roomName || '' }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="row.role === 'admin' || row.id === userStore.user?.id"
              active-text="启用"
              inactive-text="停用"
              inline-prompt
              @change="(val) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165" />
        <template #empty>
          <div class="empty-tip">暂无用户数据</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :page-sizes="[10, 20, 50]"
          :total="page.total"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadUsers"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增用户" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-row">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model="form.username" placeholder="3-20 位" />
          </el-form-item>
          <el-form-item label="初始密码" prop="password">
            <el-input v-model="form.password" placeholder="至少 6 位" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="form.role" style="width: 100%" @change="onRoleChange">
              <el-option v-for="item in ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="学号/工号">
            <el-input v-model="form.studentNo" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" />
          </el-form-item>
        </div>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <div v-if="form.role === 'student'" class="form-row">
          <el-form-item label="所在楼栋">
            <el-select v-model="form.buildingId" style="width: 100%" @change="form.roomId = null">
              <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="宿舍房间">
            <el-select v-model="form.roomId" style="width: 100%" :disabled="!form.buildingId">
              <el-option v-for="r in rooms" :key="r.id" :label="r.roomNo" :value="r.id" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import { getUsers, createUser, updateUserStatus } from '../api/user'
import { getLocations } from '../api/location'
import { userStore } from '../store/user'
import { ROLE_OPTIONS, roleLabel, roleType } from '../utils/constants'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const buildings = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const filters = reactive({ keyword: '', role: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const form = reactive({
  username: '',
  password: '',
  realName: '',
  role: 'student',
  studentNo: '',
  phone: '',
  email: '',
  buildingId: null,
  roomId: null
})

const rooms = computed(() => {
  const building = buildings.value.find((item) => item.id === form.buildingId)
  return building?.rooms || []
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度需在 3-20 位之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度需在 6-30 位之间', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function loadUsers() {
  loading.value = true
  try {
    const data = await getUsers({
      keyword: filters.keyword || undefined,
      role: filters.role || undefined,
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
  loadUsers()
}

function reset() {
  filters.keyword = ''
  filters.role = ''
  page.current = 1
  loadUsers()
}

function handleSizeChange() {
  page.current = 1
  loadUsers()
}

function openCreate() {
  Object.assign(form, {
    username: '',
    password: '123456',
    realName: '',
    role: 'student',
    studentNo: '',
    phone: '',
    email: '',
    buildingId: null,
    roomId: null
  })
  dialogVisible.value = true
}

function onRoleChange() {
  if (form.role !== 'student') {
    form.buildingId = null
    form.roomId = null
  }
}

async function submitCreate() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await createUser({ ...form })
      ElMessage.success('用户创建成功')
      dialogVisible.value = false
      loadUsers()
    } finally {
      submitting.value = false
    }
  })
}

async function toggleStatus(row, enabled) {
  const action = enabled ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定要${action}账号「${row.username}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await updateUserStatus(row.id, enabled ? 1 : 0)
    row.status = enabled ? 1 : 0
    ElMessage.success(`${action}成功`)
  } catch (e) {
    loadUsers()
  }
}

async function loadLocations() {
  try {
    buildings.value = await getLocations()
  } catch (e) {
    buildings.value = []
  }
}

onMounted(() => {
  loadLocations()
  loadUsers()
})
</script>

<style scoped>
.username {
  font-weight: 600;
  color: #1f5fd6;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 560px) {
  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
