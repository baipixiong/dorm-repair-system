<template>
  <AuthLayout>
    <div class="register-head">
      <div class="register-badge">学生注册</div>
      <h2>创建账号</h2>
      <p>注册后即可提交宿舍报修并跟踪维修进度</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="register-form">
      <div class="form-row">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" placeholder="3-20 位字母或数字" clearable />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" clearable />
        </el-form-item>
      </div>

      <div class="form-row">
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" />
        </el-form-item>
      </div>

      <div class="form-row">
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="请输入学号" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="用于维修联系" clearable />
        </el-form-item>
      </div>

      <div class="form-row">
        <el-form-item label="所在楼栋" prop="buildingId">
          <el-select v-model="form.buildingId" placeholder="请选择楼栋" style="width: 100%" @change="onBuildingChange">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="宿舍房间" prop="roomId">
          <el-select v-model="form.roomId" placeholder="请选择房间" style="width: 100%" :disabled="!form.buildingId">
            <el-option v-for="r in rooms" :key="r.id" :label="r.roomNo" :value="r.id" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="选填" clearable />
      </el-form-item>

      <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
        注 册
      </el-button>
    </el-form>

    <div class="register-foot">
      已有账号？
      <router-link to="/login">返回登录</router-link>
    </div>
  </AuthLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthLayout from '../components/AuthLayout.vue'
import { register } from '../api/auth'
import { getLocations } from '../api/location'
import { userStore } from '../store/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const buildings = ref([])

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
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

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度需在 3-20 位之间', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度需在 6-30 位之间', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function onBuildingChange() {
  form.roomId = null
}

async function loadLocations() {
  try {
    buildings.value = await getLocations()
  } catch (e) {
    buildings.value = []
  }
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const data = await register({
        username: form.username,
        password: form.password,
        realName: form.realName,
        studentNo: form.studentNo,
        phone: form.phone,
        email: form.email,
        buildingId: form.buildingId,
        roomId: form.roomId
      })
      userStore.setLogin(data)
      ElMessage.success('注册成功，已自动登录')
      router.push('/dashboard')
    } finally {
      loading.value = false
    }
  })
}

onMounted(loadLocations)
</script>

<style scoped>
.register-head {
  margin-bottom: 20px;
}

.register-badge {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: #1f5fd6;
  background: #eaf1fd;
  margin-bottom: 12px;
}

.register-head h2 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #17233a;
}

.register-head p {
  margin: 0;
  color: #7a869c;
  font-size: 13px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
  background: linear-gradient(120deg, #1f5fd6, #2f7cf6);
  border: none;
  box-shadow: 0 10px 22px rgba(31, 95, 214, 0.3);
}

.register-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: #7a869c;
}

@media (max-width: 520px) {
  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
