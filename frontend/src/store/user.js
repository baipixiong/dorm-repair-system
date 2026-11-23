import { reactive } from 'vue'

const TOKEN_KEY = 'gzhu_repair_token'
const USER_KEY = 'gzhu_repair_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch (e) {
    return null
  }
}

export const userStore = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser(),

  setLogin(data) {
    this.token = data.token
    this.user = data.user
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
  },

  setUser(user) {
    this.user = user
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  },

  clear() {
    this.token = ''
    this.user = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  },

  get role() {
    return this.user?.role || ''
  },

  get isAdmin() {
    return this.role === 'admin'
  },

  get isStudent() {
    return this.role === 'student'
  },

  get isTechnician() {
    return this.role === 'technician'
  },

  get displayName() {
    return this.user?.realName || this.user?.username || '未登录'
  }
})
