import { createRouter, createWebHistory } from 'vue-router'
import { userStore } from '../store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { guest: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { guest: true, title: '注册' }
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    meta: { auth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { auth: true, title: '工作台' }
      },
      {
        path: 'tickets',
        name: 'TicketList',
        component: () => import('../views/TicketList.vue'),
        meta: { auth: true, title: '工单管理' }
      },
      {
        path: 'tickets/create',
        name: 'TicketCreate',
        component: () => import('../views/TicketCreate.vue'),
        meta: { auth: true, roles: ['student'], title: '提交报修' }
      },
      {
        path: 'tickets/:id',
        name: 'TicketDetail',
        component: () => import('../views/TicketDetail.vue'),
        meta: { auth: true, title: '工单详情' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('../views/Statistics.vue'),
        meta: { auth: true, title: '数据统计' }
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('../views/UserManage.vue'),
        meta: { auth: true, roles: ['admin'], title: '用户管理' }
      },
      {
        path: 'locations',
        name: 'LocationManage',
        component: () => import('../views/LocationManage.vue'),
        meta: { auth: true, roles: ['admin'], title: '楼栋管理' }
      },
      {
        path: 'audit',
        name: 'AuditLog',
        component: () => import('../views/AuditLog.vue'),
        meta: { auth: true, roles: ['admin'], title: '操作日志' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { auth: true, title: '个人中心' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  if (to.meta.auth && !userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && userStore.token) {
    return '/dashboard'
  }
  if (to.meta.roles && !to.meta.roles.includes(userStore.role)) {
    return '/dashboard'
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} · 广州大学宿舍报修系统`
    : '广州大学 · 宿舍报修与工单管理系统'
})

export default router
