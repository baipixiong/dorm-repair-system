<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">个人中心</h2>
        <p class="page-desc">查看和修改个人资料，维护联系方式与宿舍信息。</p>
      </div>
    </div>

    <div class="profile-grid">
      <el-card shadow="never" class="profile-card">
        <div class="avatar-wrap">
          <el-avatar :size="76" class="big-avatar">{{ initials }}</el-avatar>
          <div class="profile-name">{{ userStore.displayName }}</div>
          <el-tag :type="roleType(userStore.role)" effect="light" round>{{ roleLabel(userStore.role) }}</el-tag>
        </div>

        <div class="profile-rows">
          <div class="profile-row"><span>登录账号</span><strong>{{ me.username }}</strong></div>
          <div class="profile-row"><span>学号/工号</span><strong>{{ me.studentNo || '-' }}</strong></div>
          <div class="profile-row"><span>手机号</span><strong>{{ me.phone || '-' }}</strong></div>
          <div class="profile-row"><span>邮箱</span><strong>{{ me.email || '-' }}</strong></div>
          <div class="profile-row"><span>所属宿舍</span><strong>{{ dormText }}</strong></div>
          <div class="profile-row"><span>注册时间</span><strong>{{ me.createdAt || '-' }}</strong></div>
        </div>
      </el-card>

      <el-card shadow="never">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本资料" name="profile">
            <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-position="top" class="tab-form">
              <div class="form-row">
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="profileForm.realName" />
                </el-form-item>
                <el-form-item label="性别">
                  <el-select v-model="profileForm.gender" style="width: 100%">
                    <el-option label="未设置" :value="0" />
                    <el-option label="男" :value="1" />
                    <el-option label="女" :value="2" />
                  </el-select>
                </el-form-item>
              </div>
              <div class="form-row">
                <el-form-item label="手机号">
                  <el-input v-model="profileForm.phone" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" />
                </el-form-item>
              </div>
              <div v-if="userStore.isStudent" class="form-row">
                <el-form-item label="所在楼栋">
                  <el-select v-model="profileForm.buildingId" style="width: 100%" @change="profileForm.roomId = null">
                    <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="宿舍房间">
                  <el-select v-model="profileForm.roomId" style="width: 100%" :disabled="!profileForm.buildingId">
                    <el-option v-for="r in profileRooms" :key="r.id" :label="r.roomNo" :value="r.id" />
                  </el-select>
                </el-form-item>
              </div>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-position="top" class="tab-form">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少 6 位" />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
              </el-form-item>
              <el-button type="primary" :loading="changing" @click="savePassword">修改密码</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateProfile, changePassword } from '../api/auth'
import { getLocations } from '../api/location'
import { userStore } from '../store/user'
import { roleLabel, roleType } from '../utils/constants'

const activeTab = ref('profile')
const profileRef = ref()
const passwordRef = ref()
const buildings = ref([])
const saving = ref(false)
const changing = ref(false)

const me = computed(() => userStore.user || {})
const initials = computed(() => (userStore.displayName || 'U').slice(0, 1))
const dormText = computed(() => {
  const user = me.value
  if (!user.buildingName && !user.roomName) return '-'
  return `${user.buildingName || ''} ${user.roomName || ''}`.trim()
})

const profileForm = reactive({
  realName: '',
  phone: '',
  email: '',
  gender: 0,
  buildingId: null,
  roomId: null
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRooms = computed(() => {
  const building = buildings.value.find((item) => item.id === profileForm.buildingId)
  return building?.rooms || []
})

const profileRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度需在 6-30 位之间', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

function fillProfile() {
  const user = me.value
  profileForm.realName = user.realName || ''
  profileForm.phone = user.phone || ''
  profileForm.email = user.email || ''
  profileForm.gender = user.gender ?? 0
  profileForm.buildingId = user.buildingId || null
  profileForm.roomId = user.roomId || null
}

async function loadLocations() {
  try {
    buildings.value = await getLocations()
  } catch (e) {
    buildings.value = []
  }
}

async function saveProfile() {
  if (!profileRef.value) return
  await profileRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const user = await updateProfile({
        realName: profileForm.realName,
        phone: profileForm.phone,
        email: profileForm.email,
        gender: profileForm.gender,
        buildingId: profileForm.buildingId,
        roomId: profileForm.roomId
      })
      userStore.setUser(user)
      ElMessage.success('资料保存成功')
    } finally {
      saving.value = false
    }
  })
}

async function savePassword() {
  if (!passwordRef.value) return
  await passwordRef.value.validate(async (valid) => {
    if (!valid) return
    changing.value = true
    try {
      await changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      ElMessage.success('密码修改成功，请重新登录')
      userStore.clear()
      window.location.href = '/login'
    } finally {
      changing.value = false
    }
  })
}

onMounted(async () => {
  fillProfile()
  await loadLocations()
  fillProfile()
})
</script>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 14px 0 22px;
  border-bottom: 1px dashed #eef2f8;
}

.big-avatar {
  background: linear-gradient(135deg, #1f5fd6, #12b3a8);
  color: #fff;
  font-size: 30px;
  font-weight: 700;
}

.profile-name {
  font-size: 18px;
  font-weight: 700;
  color: #1b2437;
}

.profile-rows {
  margin-top: 12px;
}

.profile-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 2px;
  font-size: 13px;
  border-bottom: 1px dashed #f0f3f9;
}

.profile-row:last-child {
  border-bottom: none;
}

.profile-row span {
  color: #93a0b5;
}

.profile-row strong {
  color: #1b2437;
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

.tab-form {
  max-width: 560px;
  padding-top: 8px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 980px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
