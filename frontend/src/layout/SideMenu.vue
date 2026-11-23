<template>
  <div class="side-menu">
    <div class="logo" :class="{ 'is-collapsed': collapse }" @click="goHome">
      <div class="logo-mark">
        <img :src="logoImage" alt="广州大学校徽" />
      </div>
      <div v-show="!collapse" class="logo-text">
        <div class="logo-title">广州大学</div>
      </div>
    </div>

    <el-menu
      :default-active="activeMenu"
      :collapse="collapse"
      :collapse-transition="false"
      router
      class="menu"
      @select="$emit('select')"
    >
      <el-menu-item index="/dashboard">
        <el-icon><Odometer /></el-icon>
        <template #title>工作台</template>
      </el-menu-item>

      <el-menu-item index="/tickets">
        <el-icon><Tickets /></el-icon>
        <template #title>{{ ticketMenuLabel }}</template>
      </el-menu-item>

      <el-menu-item v-if="isStudent" index="/tickets/create">
        <el-icon><EditPen /></el-icon>
        <template #title>提交报修</template>
      </el-menu-item>

      <el-menu-item index="/statistics">
        <el-icon><DataAnalysis /></el-icon>
        <template #title>{{ isAdmin ? '数据统计' : '我的统计' }}</template>
      </el-menu-item>

      <template v-if="isAdmin">
        <div v-show="!collapse" class="menu-group">系统管理</div>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/locations">
          <el-icon><OfficeBuilding /></el-icon>
          <template #title>楼栋管理</template>
        </el-menu-item>
        <el-menu-item index="/audit">
          <el-icon><Document /></el-icon>
          <template #title>操作日志</template>
        </el-menu-item>
      </template>

      <el-menu-item index="/profile">
        <el-icon><Setting /></el-icon>
        <template #title>个人中心</template>
      </el-menu-item>
    </el-menu>

    <div v-show="!collapse" class="side-foot">
      <div class="foot-line"></div>
      <div>广州大学后勤保障处</div>
      <div class="foot-sub">Dormitory Repair System</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { userStore } from '../store/user'
import logoImage from '../assets/logo.jpg'

defineProps({
  collapse: { type: Boolean, default: false }
})

defineEmits(['select'])

const route = useRoute()
const router = useRouter()

const isAdmin = computed(() => userStore.isAdmin)
const isStudent = computed(() => userStore.isStudent)

const ticketMenuLabel = computed(() => {
  if (userStore.isAdmin) return '工单管理'
  if (userStore.isTechnician) return '我的维修工单'
  return '我的报修'
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/tickets/create')) return '/tickets/create'
  if (route.path.startsWith('/tickets')) return '/tickets'
  return route.path
})

function goHome() {
  router.push('/dashboard')
}
</script>

<style scoped>
.side-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #0e1d3a 0%, #122a52 60%, #0d1f3d 100%);
  color: #fff;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 20px 18px;
  cursor: pointer;
}

.logo.is-collapsed {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

.logo-mark {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.4);
}

.logo-mark img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.logo-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 2px;
}

.menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 4px 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.menu-group {
  padding: 16px 24px 6px;
  font-size: 11px;
  letter-spacing: 1px;
  color: #5f7699;
}

.side-menu :deep(.el-menu) {
  background: transparent;
}

.side-menu :deep(.el-menu-item) {
  color: #a9bcd9;
  height: 44px;
  line-height: 44px;
  margin: 4px 12px;
  border-radius: 10px;
  font-size: 14px;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.07);
  color: #ffffff;
}

.side-menu :deep(.el-menu-item.is-active) {
  color: #ffffff;
  background: linear-gradient(90deg, rgba(47, 124, 246, 0.95), rgba(18, 179, 168, 0.8));
  box-shadow: 0 8px 18px rgba(18, 90, 210, 0.35);
}

.side-menu :deep(.el-menu-item .el-icon) {
  font-size: 17px;
}

.side-foot {
  padding: 18px 22px 22px;
  font-size: 11px;
  color: #6f86aa;
  line-height: 1.7;
}

.foot-line {
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
  margin-bottom: 12px;
}

.foot-sub {
  color: #4e6385;
  letter-spacing: 0.5px;
}
</style>
