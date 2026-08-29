import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 60000 // 🟢 改大一点：导出文件可能比较慢，改成60秒
})

// 1. 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = userStore.token
    }
    return config
  },
  (error) => {
    console.error('Request Error:', error)
    return Promise.reject(error)
  }
)

// 2. 响应拦截器
service.interceptors.response.use(
  (response) => {
    // 🟢 核心修复：如果是二进制文件流 (Blob)，直接放行！
    // 依据：前端 API 调用时设置了 responseType: 'blob'
    if (response.config.responseType === 'blob') {
      return response.data
    }

    // 2xx 状态码走这里
    const res = response.data

    // 假设后端约定：code === 200 为成功
    if (res.code === 200) {
      return res.data
    } else {
      // 业务错误处理
      ElMessage.error(res.msg || '系统繁忙，请稍后再试')

      // 特殊处理：401 未登录 / Token 过期
      if (res.code === 401) {
        ElMessageBox.confirm('登录状态已失效，您可以留在该页面，或者重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
        })
      }
      return Promise.reject(new Error(res.msg || 'Error'))
    }
  },
  (error) => {
    console.error('Response Error:', error)
    const msg = error.response?.data?.msg || error.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service
