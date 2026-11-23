<template>
  <el-container class="layout">
    <el-aside v-if="!isMobile" :width="collapsed ? '74px' : '232px'" class="aside">
      <SideMenu :collapse="collapsed" />
    </el-aside>

    <el-drawer v-model="drawerVisible" direction="ltr" size="232px" :with-header="false" class="mobile-drawer">
      <SideMenu @select="drawerVisible = false" />
    </el-drawer>

    <el-container class="body">
      <el-header class="topbar">
        <div class="topbar-left">
          <el-button text class="toggle-btn" @click="toggleSide">
            <el-icon :size="18">
              <Menu v-if="isMobile" />
              <Expand v-else-if="collapsed" />
              <Fold v-else />
            </el-icon>
          </el-button>
          <div class="crumb">
            <span class="crumb-role">{{ roleText }}</span>
            <h3>{{ pageTitle }}</h3>
          </div>
        </div>

        <div class="topbar-right">
          <div class="date-chip">
            <el-icon><Calendar /></el-icon>
            <span>{{ today }}</span>
          </div>
          <el-tag :type="roleTagType" effect="light" round>{{ roleText }}</el-tag>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-chip">
              <el-avatar :size="34" class="avatar">{{ initials }}</el-avatar>
              <div class="user-meta">
                <span class="user-name">{{ userStore.displayName }}</span>
                <span class="user-sub">{{ userStore.user?.username }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><Setting /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import SideMenu from './SideMenu.vue'
import { userStore } from '../store/user'
import { getMe } from '../api/auth'
import { roleLabel, roleType } from '../utils/constants'

const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const drawerVisible = ref(false)
const isMobile = ref(window.innerWidth < 900)

const pageTitle = computed(() => route.meta.title || '工作台')
const roleText = computed(() => roleLabel(userStore.role))
const roleTagType = computed(() => roleType(userStore.role))
const initials = computed(() => (userStore.displayName || 'U').slice(0, 1))

const today = computed(() => {
  const now = new Date()
  const weeks = ['日', '一', '二', '三', '四', '五', '六']
  return `${now.getMonth() + 1}月${now.getDate()}日 星期${weeks[now.getDay()]}`
})

function toggleSide() {
  if (isMobile.value) {
    drawerVisible.value = true
  } else {
    collapsed.value = !collapsed.value
  }
}

function onResize() {
  isMobile.value = window.innerWidth < 900
  if (!isMobile.value) {
    drawerVisible.value = false
  }
}

async function refreshUser() {
  try {
    const me = await getMe()
    userStore.setUser(me)
  } catch (e) {
    // 401 由请求拦截器统一处理
  }
}

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
      userStore.clear()
      router.push('/login')
    } catch (e) {
      // 用户取消
    }
  }
}

onMounted(() => {
  refreshUser()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.layout {
  height: 100vh;
  background: var(--page-bg);
}

.aside {
  transition: width 0.2s ease;
  overflow: hidden;
}

.body {
  min-width: 0;
}

.topbar {
  height: 68px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid #e8eef7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px 0 10px;
  z-index: 10;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.toggle-btn {
  color: #5a6a85;
  padding: 8px;
}

.crumb {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.crumb-role {
  font-size: 11px;
  color: #8b98ad;
  letter-spacing: 1px;
}

.crumb h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1b2437;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.date-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #7a869c;
  background: #f4f7fc;
  padding: 6px 12px;
  border-radius: 999px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 4px 6px 4px 4px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-chip:hover {
  background: #f3f6fb;
}

.avatar {
  background: linear-gradient(135deg, #1f5fd6, #12b3a8);
  color: #fff;
  font-weight: 600;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: #1b2437;
}

.user-sub {
  font-size: 11px;
  color: #93a0b5;
}

.content {
  padding: 0;
  overflow-y: auto;
  background: var(--page-bg);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 900px) {
  .date-chip {
    display: none;
  }

  .user-meta {
    display: none;
  }

  .topbar {
    padding-right: 14px;
  }
}
</style>
