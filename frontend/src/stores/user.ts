import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // --- State ---
  const id = ref<number>()
  const token = ref('')
  const username = ref('') // 学号/工号
  const name = ref('')     // 真实姓名
  const role = ref('')     // 角色
  const department = ref('') // 学院
  const isCadre = ref(false) // 🟢 新增：是否学生干部

  // --- Getters ---
  const isLogin = computed(() => !!token.value)

  const isStudent = computed(() => role.value === 'STUDENT')

  // 🟢 核心修改：如果是学生干部，也视为管理层，允许进入后台
  const isManagement = computed(() => {
    return ['ROOT', 'ADMIN', 'TEACHER', 'AUDITOR'].includes(role.value) || isCadre.value
  })

  // --- Actions ---
  // 🟢 login 接收 payload 增加 isCadre
  function login(payload: { id: number; tokenStr: string; name: string; dept: string; role: string; username: string; isCadre?: boolean }) {
    id.value = payload.id
    token.value = payload.tokenStr
    name.value = payload.name
    department.value = payload.dept
    role.value = payload.role
    username.value = payload.username
    isCadre.value = payload.isCadre || false // 存入状态
  }

  function logout() {
    id.value = undefined
    token.value = ''
    username.value = ''
    name.value = ''
    role.value = ''
    department.value = ''
    isCadre.value = false

    sessionStorage.clear()
    localStorage.removeItem('user')
    window.location.reload()
  }

  function hasRole(requiredRole: string): boolean {
    if (role.value === 'ROOT') return true
    if (requiredRole === 'ADMIN' && role.value === 'ADMIN') return true
    // 🟢 干部有权访问基础管理页
    if (requiredRole === 'CADRE' && isCadre.value) return true

    return role.value === requiredRole
  }

  return {
    id, token, username, name, role, department, isCadre,
    isLogin, isStudent, isManagement,
    login, logout, hasRole
  }
}, {
  persist: {
    storage: sessionStorage
  }
})
