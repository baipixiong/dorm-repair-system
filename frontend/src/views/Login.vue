<template>
  <AuthLayout>
    <div class="login-head">
      <div class="login-badge">广州大学</div>
      <h2>欢迎回来</h2>
      <p>登录宿舍报修与工单管理系统</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
      <el-form-item label="账号" prop="username">
        <el-input v-model="form.username" size="large" placeholder="请输入用户名" :prefix-icon="User" clearable />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          size="large"
          type="password"
          show-password
          placeholder="请输入密码"
          :prefix-icon="Lock"
        />
      </el-form-item>

      <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
        登 录
      </el-button>
    </el-form>

    <div class="login-foot">
      还没有账号？
      <router-link to="/register">学生注册</router-link>
    </div>
  </AuthLayout>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import AuthLayout from '../components/AuthLayout.vue'
import { login } from '../api/auth'
import { userStore } from '../store/user'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const data = await login({ ...form })
      userStore.setLogin(data)
      ElMessage.success(`欢迎回来，${data.user.realName}`)
      router.push(route.query.redirect || '/dashboard')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-head {
  margin-bottom: 26px;
}

.login-badge {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 1px;
  color: #1f5fd6;
  background: linear-gradient(120deg, #eaf1fd, #e6f7ff);
  border: 1px solid #d9e7fb;
  margin-bottom: 14px;
}

.login-head h2 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #14213c;
}

.login-head p {
  margin: 0;
  color: #7a869c;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
  height: 46px;
  font-size: 15px;
  letter-spacing: 4px;
  background: linear-gradient(120deg, #1f5fd6, #4f9cff);
  border: none;
  box-shadow: 0 10px 22px rgba(31, 95, 214, 0.3);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.submit-btn:hover {
  filter: brightness(1.05);
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(31, 95, 214, 0.38);
}

.login-foot {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: #7a869c;
}
</style>
